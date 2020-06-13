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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDVarSet;
import tau.smlab.syntech.controller.Controller;
import tau.smlab.syntech.games.util.SaveLoadWithDomains;
import tau.smlab.syntech.jtlv.BDDPackage;
import tau.smlab.syntech.jtlv.BDDPackage.BBDPackageVersion;
import tau.smlab.syntech.jtlv.Env;
import tau.smlab.syntech.jtlv.env.module.ModuleBDDField;

public class ControllerExecutor {
	
	protected BDD currentState = null;
	
	protected Map<String, String[]> sysVars;
	protected Map<String, String[]> envVars;
	
	protected BDDVarSet vars;
	
	protected Controller controller;
	
	/**
	 * Gets a list of possible next states according to current controller state and inputs.
	 * This method may change the inner state of the controller and therefore is not recommended to use for the time being!
	 * 
	 * @param inputs map of environment variable names to values
	 * @param maxStates maximum number of states to return
	 * @return a list of possible next states as strings
	 */
	public List<String> getNextStates(Map<String, String> inputs, int maxStates) {

		BDD inputsBDD = getInputsBDD(inputs);
		
		// TODO: problematic function because next function has side effects
		BDD nextStates = controller.next(currentState, inputsBDD);
		
		BDDIterator iter = nextStates.iterator(Env.globalUnprimeVars());
		List<String> states = new ArrayList<>();

		while (iter.hasNext() && states.size() < maxStates) {
			BDD next = iter.next();
			states.add(Env.toNiceString(next));
			next.free();
		}
		
		return states;

	}
	
	/**
	 * Instantiates a new symbolic controller executor.
	 * @param controller instance of the controller interface
	 * @param folder location of the controller files
	 * 
	 * @throws IOException if the folder does not contain the controller files
	 */
	public ControllerExecutor(Controller controller, String folder) throws IOException {
		this(controller, folder, false);
	}
	
	/**
	 * Instantiates a new symbolic controller executor.
	 * @param controller instance of the controller interface
	 * @param folder location of the controller files
	 * @param reordering whether to turn on bdd reordering during the execution. Reordering may
	 * result in better performance generally but causes spikes (a very long single step) once in a while
	 * 
	 * @throws IOException if the folder does not contain the controller files
	 */
	public ControllerExecutor(Controller controller, String folder, boolean reordering) throws IOException {
		
		BDDPackage.setCurrPackage(BDDPackage.CUDD, BBDPackageVersion.CUDD_3_0); 
		sysVars = new HashMap<>();
		envVars = new HashMap<>();
	    SaveLoadWithDomains.loadStructureAndDomains(folder + File.separator + SaveLoadWithDomains.VARS_FILE, sysVars, envVars);
	    
	    sysVars.entrySet().removeIf(var -> var.getKey().startsWith("util_"));
	    sysVars.entrySet().removeIf(var -> var.getKey().startsWith("sfa_states"));
	    envVars.entrySet().removeIf(var -> var.getKey().startsWith("util_"));
	    sysVars.entrySet().removeIf(var -> var.getKey().startsWith("sfa_states"));
		
		this.controller = controller;
		this.controller.load(folder);
		
		if (reordering) {
			Env.enableReorder();
		} else {
			Env.disableReorder();
		}
		
		vars = Env.getEmptySet();
		for (String sysVar : sysVars.keySet()) {
			vars.unionWith(Env.getVar(sysVar).getDomain().set());
		}
		for (String envVar : envVars.keySet()) {
			vars.unionWith(Env.getVar(envVar).getDomain().set());
		}
	}
	
	/**
	 * Returns the set of all output (system controlled) variables' names
	 * @return the set of all output (system controlled) variables' names
	 */
	public Map<String, String[]> getSysVars() {
		return sysVars;
	}

	/**
	 * Returns the set of all input (environment controlled) variables' names
	 * @return the set of all input (environment controlled) variables' names
	 */
	public Map<String, String[]> getEnvVars() {
		return envVars;
	}
	
	/**
	 * Frees the controller at the end of the execution
	 */
	public void free() {
		controller.free();
		currentState.free();
		vars.free();
	}
	
	/**
	 * Picks a valid initial state according to initial inputs from the environment.
	 * If the current state is a deadlock for the environment, i.e., the environment
	 * has no choice for inputs that satisfy the assumptions, no initial state is picked.
	 * 
	 * @param initialInputs map of environment variable names to values
	 * @throws IllegalStateException if the current state is an environment deadlock
	 * @throws IllegalArgumentException if the current state is not an environment deadlock and the inputs violate the safety assumptions of the environment
	 */
	public void initState(Map<String, String> initialInputs) throws IllegalStateException, IllegalArgumentException {
		
		if (currentState != null) {
			throw new IllegalStateException("Controller has already been initialized");
		}
		
		BDD inputsBDD = getInputsBDD(initialInputs);
		
		// Checks whether the controller execution has reached a state which is a deadlock for the environment, i.e.,
		// a state from which the environment has no choice for next inputs that satisfy the safety assumptions
		if (controller.initial().isZero()) {
			throw new IllegalStateException("The environment is in an initial deadlock state. There is no possible input for the environment");
		}
		
		if (controller.initial().and(inputsBDD).isZero()) {
			throw new IllegalArgumentException("The inputs are an initial violation for the environment");
		}
		
		currentState = Env.randomSat(controller.initial().and(inputsBDD), vars);
		controller.init(currentState);
		
		inputsBDD.free();
	}
	
	/**
	 * Picks a valid next state according to the controller and the next inputs from the environment. 
	 * If the current state is a deadlock for the environment, i.e., the environment
	 * has no choice for inputs that satisfy the assumptions, no next state is picked.
	 * 
	 * @param inputs map of environment variable names to values
	 * @throws IllegalStateException if the current state is an environment deadlock
	 * @throws IllegalArgumentException if the current state is not an environment deadlock and the inputs violate the safety assumptions of the environment
	 */
	public void updateState(Map<String, String> inputs) throws IllegalStateException, IllegalArgumentException {
		
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
		currentState = Env.randomSat(nextStates, vars);
		
		// TODO: satOne currently not working
//		currentState = nextStates.satOne(Env.globalUnprimeVars());
		
		nextStates.free();
		inputsBDD.free();
	}
	
	/**
	 * 
	 * @param inputs
	 */
	private BDD getInputsBDD(Map<String, String> inputs) throws IllegalArgumentException {
		
		BDD inputsBDD = Env.TRUE();
		for (String varName : inputs.keySet()) {
			BDD valBdd = Env.getBDDValue(varName, inputs.get(varName));
			if (valBdd == null) {
				throw new IllegalArgumentException("Invalid variable name or value");
			}
			if (!envVars.containsKey(varName)) {
				throw new IllegalArgumentException("The variable name " + varName + " does not refer to an input variable");
			}
			
			inputsBDD.andWith(valBdd.id());
//			valBdd.free();
			//inputsBDD = inputsBDD.and(valBdd);
		}
		return inputsBDD;
	}

	
	/**
	 * Returns the current value of the specified variable name (either an input or an output)
	 *
	 * @param varName The variable name
	 * @return The variable value
	 * @throws IllegalArgumentException if the variable name does not exist in the controller
	 * @throws IllegalStateException if the current state is zero
	 */
	public String getCurrValue(String varName) throws IllegalArgumentException, IllegalStateException {

		if (currentState.isZero()) {
			throw new IllegalStateException("Current state is zero. Check if controller is in deadlock");
		}

		ModuleBDDField field = Env.getVar(varName);
		if (field == null) {
			throw new IllegalArgumentException("The variable name " + varName + " does not exist in the controller");
		} 

		BDDDomain domain = field.getDomain();
		return Env.getValueByDomain(currentState, domain);
	}
	
	private Map<String, String> getModuleValues(Set<String> moduleVarNames) throws IllegalArgumentException, IllegalStateException {
		Map<String, String> varToVal = new HashMap<>();
		for (String varName : moduleVarNames) {
			varToVal.put(varName, getCurrValue(varName));
		}
		return varToVal;
	}
	
	/**
	 * Returns a mapping of all system variables' names to their current values
	 * @return a mapping of all system variables' names to their current values
	 */
	public Map<String, String> getCurrOutputs() throws IllegalArgumentException {
		return getModuleValues(sysVars.keySet());
	}

	/**
	 * Returns a mapping of all environment variables' names to their current values
	 * @return a mapping of all environment variables' names to their current values
	 */
	public Map<String, String> getCurrInputs() throws IllegalArgumentException {
		return getModuleValues(envVars.keySet());
	}
	
	/**
	 * Returns a mapping of all specified variables' names to their current values.
	 * Each variable name may refer to either an input or an output variable
	 * 
	 * @param varNames the variables' names that will be mapped to their current values
	 * @return the variables' values as a map between variable name and its value
	 * @throws IllegalArgumentException if there is a variable name that does not exist in the controller
	 */
	public Map<String, String> getCurrValues(String ... varNames) throws IllegalArgumentException {
		return getModuleValues(new HashSet<>(Arrays.asList(varNames)));
	}

}
