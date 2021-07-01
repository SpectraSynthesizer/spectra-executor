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

package tau.smlab.syntech.controller.jit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.javabdd.BDD;
import tau.smlab.syntech.jtlv.Env;

/**
 * A class for the execution of symbolic controllers in a Just-in-time fashion
 * @author ilia
 *
 */
public class BasicJitController implements JitController {

	private JitContext jitContext;
	private JitState jitState;
		
	private List<BDD> justiceGar = new ArrayList<>();
	private List<BDD> justiceAsm = new ArrayList<>();
	
	private List<Integer> skipped;
	
	public BasicJitController(Integer ... skipped) {
		this.skipped = Arrays.asList(skipped);
	}
	
	@Override
	public BDD next(BDD currentState, BDD inputs) {
		
		BDD currAndTrans = jitContext.getTrans().id();
		currAndTrans.andWith(currentState.id());
		currAndTrans.andWith(Env.prime(inputs));
		BDD nextStates = Env.FALSE();
		
		if (jitState.getRank() == 0) {
			
			// We divide between guarantee satisfaction and assumption violation
			// This is in contrast to the original construction where the controller could decide to move to next zn
			// or to stay in same zn and violate some assumption. We want to be more eager so we decide that once
			// the controller can move to next zn it will do it
			
			BDD currAndJustice = justiceGar.get(jitState.getJx()).and(currentState);
			if (!currAndJustice.isZero()) {
				
				//System.out.println("rho1 with j=" + jitState.getJx());
				jitState.updateJx();
				
				// It is guaranteed to stop on some r because Y[j][r_j] = Z
				
				BDD temp;
				for (int r = 0; r < jitContext.rank(jitState.getJx()); r++) {
					temp = currAndTrans.and(jitContext.Y(jitState.getJx(), r));
					
					if (!temp.isZero()) {
						nextStates = temp;
						jitState.setRank(r);
						//System.out.println("rho1 with new j=" + jitState.getJx() + " and next r=" + jitState.getRank());
						break;
					}
					
					temp.free();
				}
				
			} else {
					
				BDD currentStatePrimed = Env.prime(currentState);
				BDD temp;
				
				for (int i = 0; i < justiceAsm.size(); i++) {
					temp = jitContext.X(jitState.getJx(), i, 0).and(currentStatePrimed);
					
					if (!temp.isZero()) {
						temp.free();
						nextStates = currAndTrans.and(jitContext.X(jitState.getJx(), i, 0));
						//System.out.println("rho3 with j=" + jitState.getJx() + " and r=0 and i=" + i);
						break;
					}
					
					temp.free();
				}
				
				currentStatePrimed.free();
			}
			
			currAndJustice.free();

		} else {
			
			// Find lowest rank
			BDD candidate = currAndTrans.and(jitContext.Y(jitState.getJx(), jitState.getRank()-1));
			
			if (candidate.isZero()) {

				BDD currentStatePrimed = Env.prime(currentState);
				BDD temp;
				
				for (int i = 0; i < justiceAsm.size(); i++) {
					temp = jitContext.X(jitState.getJx(), i, jitState.getRank()).and(currentStatePrimed);
					
					if (!temp.isZero()) {
						temp.free();
						nextStates = currAndTrans.and(jitContext.X(jitState.getJx(), i, jitState.getRank()));
						//System.out.println("rho3 with j=" + jitState.getJx() + " and r=" + jitState.getRank() + " and i=" + i);
						break;
					}
					
					temp.free();
				}

				currentStatePrimed.free();	
				
			} else {
				
				// It is guaranteed to stop on some r because Y[j][r_j] = Z
				BDD temp;
				for (int r = 0; r < jitContext.rank(jitState.getJx()); r++) {
					temp = currAndTrans.and(jitContext.Y(jitState.getJx(), r));
					
					if (!temp.isZero()) {
						//System.out.println("rho2 with j=" + jitState.getJx() + " and r=" + jitState.getRank() + " and next r="+ r);
						nextStates = temp;
						jitState.setRank(r);
						break;
					}
					
					temp.free();
				}
			}
			
			candidate.free();
		}

		currAndTrans.free();
	    BDD primedNextStates = nextStates.exist(Env.globalUnprimeVars());
	    nextStates.free();
	    nextStates = Env.unprime(primedNextStates);
	    primedNextStates.free();
	    return nextStates;
	}
	
	protected void loadTransAndInitial(BDD trans) {
		
		BDD temp = Env.getVar("util_0").getDomain().ithVar(0);
		temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(0));
		BDD sysIni = trans.restrict(temp);
		temp.free();
		
		temp = Env.getVar("util_0").getDomain().ithVar(1);
		temp.andWith(Env.getVar("util_In").getDomain().ithVar(0));
		BDD envIni = trans.restrict(temp);
		temp.free();
		
		temp = Env.getVar("util_0").getDomain().ithVar(0);
		temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(1));
		BDD sysTrans = trans.restrict(temp);
		temp.free();
		
		temp = Env.getVar("util_0").getDomain().ithVar(1);
		temp.andWith(Env.getVar("util_In").getDomain().ithVar(1));
		BDD envTrans = trans.restrict(temp);
		temp.free();
		
		jitContext = new JitContext(envIni, envTrans, sysIni, sysTrans);
	}
	
	protected void loadFixpoints(BDD fixpoints, int[] ranks, int n, int m) {

		BDD[][][] X = new BDD[n-skipped.size()][m][];
		int sk = 0;
		
		for (int j = 0; j < n; j++) {
			
			if (!skipped.contains(j)) {
				for (int i = 0; i < m; i++) {
					X[j-sk][i] = new BDD[ranks[j]];
					for (int r = 0; r < ranks[j]; r++) {
						
						BDD temp = Env.getVar("util_In").getDomain().ithVar(i);
						temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(j));
						temp.andWith(Env.getVar("util_Rn").getDomain().ithVar(r));
						
						BDD XBDD = fixpoints.restrict(temp);
						X[j-sk][i][r] = Env.prime(XBDD);
						XBDD.free();
						temp.free();
					}
				}
			} else {
				sk++;
			}
		}
		
		// Extract Y from X on current r
		sk = 0;
		BDD[][] Y = new BDD[n-skipped.size()][];
		for (int j = 0; j < n; j++) {
			
			if (!skipped.contains(j)) {
				Y[j-sk] = new BDD[ranks[j]];
				for (int r = 0; r < ranks[j]; r++) {
					Y[j-sk][r] = Env.FALSE();
					for (int i = 0; i < m; i++) {
						Y[j-sk][r].orWith(X[j-sk][i][r].id());
					}
				}
			} else {
				sk++;
			}
		}
		
		jitContext.setMX(X);
		jitContext.setMY(Y);
	}
	
	protected void loadJustices(BDD justices, int n, int m) {
		
		BDD justice;
		for (int j = 0; j < n; j++) {
			
			if (!skipped.contains(j)) {
				BDD temp = Env.getVar("util_0").getDomain().ithVar(0);
				temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(j));
				
				justice = justices.restrict(temp);
				temp.free();
	            
	            justiceGar.add(justice);	
			}
		}
		
		for (int i = 0; i < m; i++) {
			
			BDD temp = Env.getVar("util_0").getDomain().ithVar(1);
			temp.andWith(Env.getVar("util_In").getDomain().ithVar(i));
			
			justice = justices.restrict(temp);
			temp.free();
            
			justiceAsm.add(justice);
		}
	}
	
	@Override
	public void load(String folder) {
		
		try {
			
			BufferedReader sizesReader = new BufferedReader(new FileReader(folder + File.separator + "sizes"));
			
			int n = Integer.parseInt(sizesReader.readLine());
			int m = Integer.parseInt(sizesReader.readLine());
			int[] ranks = new int[n];
			for (int j = 0; j < n; j++) {
				ranks[j] = Integer.parseInt(sizesReader.readLine());
			}
			
			sizesReader.close();
			System.out.println(Env.TRUE().getFactory().getNodeNum() + " - Read Sizes");

			
			// Extract justices
			
			BDD justices = Env.loadBDD(folder + File.separator + "justice.bdd");
			loadJustices(justices, n, m);	
			justices.free();
			System.out.println(Env.TRUE().getFactory().getNodeNum() + " - Loaded Justice BDD");
			
			
			// Extract trans and init
			
			BDD trans = Env.loadBDD(folder + File.separator + "trans.bdd");
			loadTransAndInitial(trans);
			trans.free();
			System.out.println(Env.TRUE().getFactory().getNodeNum() + " - Loaded Transition BDD");
			
			
			// Extract X from fixpoints BDD

			BDD fixpoints = Env.loadBDD(folder + File.separator + "fixpoints.bdd");
			loadFixpoints(fixpoints, ranks, n, m);
			fixpoints.free();
			System.out.println(Env.TRUE().getFactory().getNodeNum() + " - Loaded Fixed-Points BDD");
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Delete vars even though they are not really removed from bdd engine.
		// At least they won't show up in next states enumeration
		Env.deleteVar("util_In");
		Env.deleteVar("util_Jn");
		Env.deleteVar("util_Rn");
		Env.deleteVar("util_0");
		
		BDD Z = Env.unprime(jitContext.Y(0, jitContext.rank(0) - 1));
		
		jitContext.setTrans(jitContext.getSysTrans().and(jitContext.getEnvTrans()));
		jitContext.setIni(jitContext.getSysIni().and(jitContext.getEnvIni()));
		
		BDD tempIni = jitContext.getIni().exist(Env.globalPrimeVars());
		jitContext.getIni().free();
		
		jitContext.setIni(tempIni.and(Z));
		tempIni.free();
		Z.free();
	}

	@Override
	public List<BDD> getJusticeGar() {
		return justiceGar;
	}

	@Override
	public List<BDD> getJusticeAsm() {
		return justiceAsm;
	}

	@Override
	public JitContext getJitContext() {
		return jitContext;
	}

	@Override
	public void free() {
		
		jitContext.free();
		
		for (int i = 0; i < justiceAsm.size(); i++) {
			justiceAsm.get(i).free();
		}
		
		for (int i = 0; i < justiceGar.size(); i++) {
			justiceGar.get(i).free();
		}
	}

	@Override
	public BDD transitions() {
		return jitContext.getTrans();
	}

	@Override
	public JitState getJitState() {
		return jitState;
	}

	@Override
	public BDD initial() {
		return jitContext.getIni();
	}

	@Override
	public void init(BDD currentState) {
		
		jitState = new JitState();
		BDD currentStatePrimed = Env.prime(currentState);
						
		boolean found = false;
		BDD temp;
		for (int r = 0; r < jitContext.rank(0); r++) {
			temp = currentStatePrimed.and(jitContext.Y(0, r));
			
			if (!temp.isZero()) {
				found = true;
				temp.free();
				jitState.setRank(r);
				break;
			}
			
			temp.free();
		}
		
		if (!found) {
			throw new IllegalArgumentException("Probably initial environment inputs violate the initial assumptions");
		}
		
		currentStatePrimed.free();
		
		jitState.setGoalFinder(new NextJusticeGoalFinder() {
			
			@Override
			public int findNextJusticeGoal(int jx) {
				return (jx + 1) % justiceGar.size();
			}
		});
		
		jitContext.getEnvIni().free();
		jitContext.getSysIni().free();
	}
}
