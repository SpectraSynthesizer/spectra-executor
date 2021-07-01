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

package tau.smlab.syntech.controller.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import tau.smlab.syntech.controller.Controller;
import tau.smlab.syntech.jtlv.Env;

public class FlexibleControllerExecutor extends ControllerExecutor {
	
	boolean waitingForChoice = false;
	List<Map<String, String>> choices = new ArrayList<>();
	int maxStates = 100;

	public FlexibleControllerExecutor(Controller controller, String folder) throws IOException {
		super(controller, folder);
	}
	
	public void updateState(Map<String, String> inputs) throws IllegalStateException, IllegalArgumentException {
		
		if (waitingForChoice) {
			throw new IllegalStateException("Cannot update state before next choice was received");
		}
		
		if (currentState == null) {
			throw new IllegalStateException("Controller has not yet been initialized");
		}
		
		BDD inputsBDD = getInputsBDD(inputs);
		
		BDD currAndTrans = controller.transitions().id();
		currAndTrans.andWith(currentState.id());
		
		if (currAndTrans.isZero()) {
			throw new IllegalStateException("The environment is in a deadlock state. There is no possible safe input for the environment");
		}
		currAndTrans.andWith(Env.prime(inputsBDD));
		if (currAndTrans.isZero()) {
			throw new IllegalArgumentException("The inputs are a safety violation for the environment");
		}
		currAndTrans.free();
		
		BDD nextStates = controller.next(currentState, inputsBDD);

	    currentState.free();
	    
	    buildChoices(nextStates);
	    waitingForChoice = true;
		
		nextStates.free();
		inputsBDD.free();
	}
	
	public List<Map<String, String>> getChoices() {
		
		return choices;
	}
	
	public void chooseNextState(Map<String, String> nextState) {

		BDD nextStateBDD = Env.TRUE();
		for (String varName : nextState.keySet()) {
			BDD valBdd = Env.getBDDValue(varName, nextState.get(varName));
			if (valBdd == null) {
				throw new IllegalArgumentException("Invalid variable name or value");
			}
			nextStateBDD.andWith(valBdd.id());
		}
		
		currentState = nextStateBDD;
		waitingForChoice = false;
		choices.clear();
	}
	
	private void buildChoices(BDD nextStates) {
		
		choices.clear();
		
		BDDIterator iter = nextStates.iterator(vars);
		
		Set<String> allKeySet = new HashSet<>();
		allKeySet.addAll(envVars.keySet());
		allKeySet.addAll(sysVars.keySet());

		while (iter.hasNext() && choices.size() < maxStates) {
			BDD nextState = iter.next();
			
			Map<String, String> singleAssignment = new HashMap<String, String>();
			
			for (String varName : allKeySet) {
				List<String> values = Env.getValueNames(varName);
				
				for (String value : values) {
					BDD nextStateRestricted = nextState.restrict(Env.getBDDValue(varName, value));
					if (!nextStateRestricted.isZero()) {
						singleAssignment.put(varName, value);
						nextStateRestricted.free();
						break;
					}
					
					nextStateRestricted.free();
				}
			}
			
			choices.add(singleAssignment);
		}
	}

}
