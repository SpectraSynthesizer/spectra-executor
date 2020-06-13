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

package tau.smlab.syntech.controller.jit.cached;

import tau.smlab.syntech.controller.jit.BasicJitController;

/**
 * 
 * @author Ilia
 * Warning: obsolete class!
 *
 */
public class CachedJitController extends BasicJitController {
	
//	protected void loadFixpoints(BDD fixpoints, int[] ranks, int n, int m) {
//		
//		jitMemory = new JitMemoryCached(fixpoints, m, ranks, 50);
//		
//		BDD Z = Env.unprime(getJitMemory().Y(0, getJitMemory().rank(0) - 1));
//		
//		getJitTrans().setTrans(getJitTrans().getSysTrans().and(getJitTrans().getEnvTrans()));
//		getJitTrans().setIni(getJitTrans().getSysIni().and(getJitTrans().getEnvIni()).exist(Env.globalPrimeVars()).and(Z));
//		
//		Z.free();
//	}
//	
//	@Override
//	public void free() {
//		super.free();
//		((JitMemoryCached)jitMemory).free();
//	}
//	
//	@Override
//	public void load(String folder) {
//		
//		System.out.println(Env.TRUE().getFactory().getNodeNum() + " BEGIN LOAD");
//		
//		try {
//			
//			BufferedReader sizesReader = new BufferedReader(new FileReader(folder + File.separator + "sizes"));
//			
//			int n = Integer.parseInt(sizesReader.readLine());
//			int m = Integer.parseInt(sizesReader.readLine());
//			int[] ranks = new int[n];
//			for (int j = 0; j < n; j++) {
//				ranks[j] = Integer.parseInt(sizesReader.readLine());
//			}
//			
//			sizesReader.close();
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER READING SIZES");
//
//			
//			// Extract justices
//			
//			BDD justices = Env.loadBDD(folder + File.separator + "justice.bdd");
//			loadJustices(justices, n, m);	
//			justices.free();
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER READING JUSTICES");
//			
//			
//			// Extract trans and init
//			
//			BDD trans = Env.loadBDD(folder + File.separator + "trans.bdd");
//			loadTransAndInitial(trans);
//			trans.free();
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER READING TRANS");
//			
//			
//			// Extract X from fixpoints BDD
//
//			BDD fixpoints = Env.loadBDD(folder + File.separator + "fixpoints.bdd");
//			loadFixpoints(fixpoints, ranks, n, m);
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER READING FIX POINTS");
//			
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println(Env.TRUE().getFactory().getNodeNum() + " END LOAD");
//	}
}
