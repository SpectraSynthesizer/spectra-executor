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

/**
 * 
 * @author User
 * 
 * Warning: obsolete class!
 */
public class JitMemoryCached /*implements JitMemory*/ {
	
//	private BDD fixpoints;
//	private int[] ranks;
//	private int m;
//	private int size;
//	
//	public JitMemoryCached(BDD fixpoints, int m, int[] ranks, int size) {
//		this.fixpoints = fixpoints;
//		this.m = m;
//		this.ranks = ranks;
//		this.size = size;
//	}
//	
//	private Map<Integer, BDD> cache = new LinkedHashMap<Integer, BDD>(size + 1, .75F, true) {
//
//		private static final long serialVersionUID = -5702276897872800744L;
//
//		// This method is called just after a new entry has been added
//	    public boolean removeEldestEntry(Map.Entry<Integer, BDD> eldest) {
//	    	if (size() > size) {
//	    		
//	    		System.out.println(Env.TRUE().getFactory().getNodeNum() + " REMOVE ELDEST ENTRY BEFORE");
//	    		eldest.getValue().free();
//	    		System.out.println(Env.TRUE().getFactory().getNodeNum() + " REMOVE ELDEST ENTRY AFTER");
//	    		return true;
//	    	}
//	        return false;
//	    }
//	};
//
//	@Override
//	public BDD X(int j, int i, int r) {
//		
//		int key = key(j, i, r);
//		BDD res = cache.get(key);
//		
//		System.out.println(String.format("%d looking for key X(%d,%d,%d)",Env.TRUE().getFactory().getNodeNum(),j,i,r));
//		
//		if (res == null) {
//			
//			System.out.println("key not in cache");
//			BDD temp = Env.getVar("util_In").getDomain().ithVar(i);
//			temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(j));
//			temp.andWith(Env.getVar("util_Rn").getDomain().ithVar(r));
//			
//			BDD XBDD = fixpoints.restrict(temp);
//			res = Env.prime(XBDD);
//			XBDD.free();
//			temp.free();
//			
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER EXTRACT");
//
//			cache.put(key, res);
//		}
//		
//		return res;
//	}
//
//	@Override
//	public BDD Y(int j, int r) {
//		
//		int key = key(j, m, r);
//		BDD res = cache.get(key);
//		
//		System.out.println(String.format("%d looking for key Y(%d,%d)",Env.TRUE().getFactory().getNodeNum(),j,r));
//		
//		if (res == null) {
//			System.out.println("key not in cache");
//			res = Env.FALSE();
//			
//			for (int i = 0; i < m; i++) {
//				
////				BDD temp = Env.getVar("util_In").getDomain().ithVar(i);
////				temp.andWith(Env.getVar("util_Jn").getDomain().ithVar(j));
////				temp.andWith(Env.getVar("util_Rn").getDomain().ithVar(r));
////				
////				BDD XBDD = fixpoints.restrict(temp);
////				BDD Xp = Env.prime(XBDD);
////				res.orWith(Xp);
////				Xp.free();
////				XBDD.free();
//				
//				res.orWith(X(j,i,r).id());
//			}
//			
//			System.out.println(Env.TRUE().getFactory().getNodeNum() + " AFTER EXTRACT");
//			
//			cache.put(key, res);
//		}
//		
//		return res;
//	}
//
//	@Override
//	public int rank(int j) {
//		return ranks[j];
//	}
//	
//	private int key(int j, int i, int r) {
//		int temp = (j + r) * (j + r + 1) / 2 + r;
//		return (temp + i) * (temp + i + 1) / 2 + i;
//	}
//
//	public void free() {
//		fixpoints.free();
//	}
}
