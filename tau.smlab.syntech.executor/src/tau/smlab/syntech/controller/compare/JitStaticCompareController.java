/*
Copyright (c) since 2015, Tel Aviv University and Software Modeling Lab

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Tel Aviv University and Software Modeling Lab nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Tel Aviv University and Software Modeling Lab 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
*/

package tau.smlab.syntech.controller.compare;

import java.util.ArrayList;
import java.util.List;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import tau.smlab.syntech.controller.Controller;
import tau.smlab.syntech.controller.StaticController;
import tau.smlab.syntech.controller.jit.BasicJitController;
import tau.smlab.syntech.jtlv.Env;

public class JitStaticCompareController implements Controller {
	
	private StaticController staticController = new StaticController();
	private BasicJitController jitController;
	
	public JitStaticCompareController(BasicJitController jitController) {
		this.jitController = jitController;
	}
	
	@Override
	public void load(String folder) {
		
		jitController.load(folder);
		staticController.load(folder);
	}

	@Override
	public BDD next(BDD currentState, BDD inputs) {
				
		BDD currentStateTmp = currentState.exist(Env.getVar("Zn").support());
		
		int prevJx = jitController.getJitState().getJx();
		BDD nextJitStates = jitController.next(currentStateTmp, inputs);
		
		BDD statesToReturn = nextJitStates.id();
		nextJitStates.andWith(Env.getVar("Zn").getDomain().ithVar(jitController.getJitState().getJx()));
				
		BDD nextOrigStates = staticController.next(currentStateTmp.and(Env.getVar("Zn").getDomain().ithVar(prevJx)), inputs);		
		currentStateTmp.free();
				
		// Use this spot to compare next states
				
		BDDIterator jitIter = nextJitStates.iterator(Env.globalUnprimeVars());
		List<BDD> jitStates = new ArrayList<>();

		while (jitIter.hasNext()) {
			BDD next = jitIter.next();
			jitStates.add(next);
		}
		
		BDDIterator origIter = nextOrigStates.iterator(Env.globalUnprimeVars());
		List<BDD> origStates = new ArrayList<>();

		while (origIter.hasNext()) {
			BDD next = origIter.next();
			origStates.add(next);
		}
		
//		System.out.println("JIT states size: " + jitStates.size());
//		System.out.println("Orig states size: " + origStates.size());
		
		boolean mismatch = false;
		for (BDD jitState : jitStates) {
			if (!origStates.contains(jitState)) {
				System.out.println("JIT state not in original states!");
				System.out.println(jitState);
				System.out.println();

				mismatch = true;
			}
		}

		if (mismatch) {
			System.out.println("Original states:");
			for (BDD origState : origStates) {
				System.out.println(origState);
			}
			throw new RuntimeException();
		}
		
		for (BDD jitState : jitStates) {
			jitState.free();
		}
		
		for (BDD origState : origStates) {
			origState.free();
		}
		nextJitStates.free();
		nextOrigStates.free();
		
		return statesToReturn;
	}

	@Override
	public void free() {
		jitController.free();
		staticController.free();
	}

	@Override
	public void init(BDD currentState) {
		staticController.init(currentState);
		jitController.init(currentState);
	}

	@Override
	public BDD transitions() {
		return jitController.transitions();
	}

	@Override
	public BDD initial() {
		return jitController.initial();
	}
}
