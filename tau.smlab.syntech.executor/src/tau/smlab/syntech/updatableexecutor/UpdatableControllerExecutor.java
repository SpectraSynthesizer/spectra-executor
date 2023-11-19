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

package tau.smlab.syntech.updatableexecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDVarSet;
import tau.smlab.syntech.games.controller.Controller;
import tau.smlab.syntech.controller.executor.ControllerExecutor;
import tau.smlab.syntech.games.util.SaveLoadWithDomains;
import tau.smlab.syntech.jtlv.Env;
import tau.smlab.syntech.jtlv.ModuleVariableException;
import tau.smlab.syntech.jtlv.env.module.ModuleBDDField;

public class UpdatableControllerExecutor extends ControllerExecutor {

	protected final static String BRIDGE_DIR = "bridge";
	protected final static String SWITCH_VAR = "switch";
	protected final static String ALLOWED_VAR = "allowed";
	protected final static String BRIDGE_VARS = "bridgeVars.doms";
	protected final static String SYS_TRANS = "sysTrans.bdd";
	protected final static String CURRENT = "current.bdd";

	protected String outFolder;
	protected String bridgeFolder;
	protected boolean reordering;
	protected String name;

	protected DynamicUpdateServer server;

	protected boolean bridge = false;
	protected ReentrantLock updateLock;
	protected boolean switchLock;

	protected int bridgeCount;
	protected int curBridge;
	protected int stepCount;

	protected BDD bridgeTrans;
	BDDVarSet bridgeVars;

	public UpdatableControllerExecutor(Controller controller, String folder, String name, int port) throws IOException, ModuleVariableException {
		this(controller, folder, name, port, false);
	}

	public UpdatableControllerExecutor(Controller controller, String folder, String name, int port, boolean reordering) throws IOException, ModuleVariableException {
		super(controller, folder, name, reordering);

		this.curBridge = -1;
		stepCount = 0;

		this.outFolder = folder + File.separator;
		this.name = name;
		this.bridgeFolder = this.outFolder + BRIDGE_DIR + File.separator;
		this.reordering = reordering;
		
		BDD sysTrans = this.getController().transitions().id();
	    Env.saveBDD(this.outFolder + SYS_TRANS, sysTrans, true);
	    sysTrans.free();

		createBridgeVars();

		updateLock = new ReentrantLock();
		switchLock = false;

		this.server = new DynamicUpdateServer(this, port);
		this.server.start();
	}
	
	public Controller getController() {
		return this.controller;
	}
	
	public void printUpdateError(String errorMsg) {
		System.out.println("Update failed: " + errorMsg);
	}
	
	void addTestData(String msg) {
		try
		{
		    String filename= "test_results.csv";
		    FileWriter fw = new FileWriter(filename, true); //the true will append the new data
		    fw.write(msg);//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
	}
	
	static void printControllerLog(String msg) {	
		try
		{
		    String filename= "controller_log.txt";
		    FileWriter fw = new FileWriter(filename, true); //the true will append the new data
		    fw.write(msg + "\n");//appends the string to the file
		    fw.close();
		}
		catch(IOException ioe)
		{
		    System.err.println("IOException: " + ioe.getMessage());
		}
	}
	
	void createBridgeVars() throws IOException, ModuleVariableException {
		ModuleBDDField switch_var = Env.newVar(SWITCH_VAR);
		ModuleBDDField allowed_var = Env.newVar(ALLOWED_VAR);

		Env.addBDDValueLookup(switch_var.getName(), "false", switch_var.getDomain().ithVar(0));
		Env.addBDDValueLookup(switch_var.getName(), "true", switch_var.getDomain().ithVar(1));			
		Env.addBDDValueLookup(switch_var.prime().getName(), "false", switch_var.prime().getDomain().ithVar(0));
		Env.addBDDValueLookup(switch_var.prime().getName(), "true", switch_var.prime().getDomain().ithVar(1));

		Env.addBDDValueLookup(allowed_var.getName(), "false", allowed_var.getDomain().ithVar(0));
		Env.addBDDValueLookup(allowed_var.getName(), "true", allowed_var.getDomain().ithVar(1));			
		Env.addBDDValueLookup(allowed_var.prime().getName(), "false", allowed_var.prime().getDomain().ithVar(0));
		Env.addBDDValueLookup(allowed_var.prime().getName(), "true", allowed_var.prime().getDomain().ithVar(1));

		FileOutputStream fos = new FileOutputStream(this.outFolder + BRIDGE_VARS);
		ObjectOutputStream oos = new ObjectOutputStream(fos);

		ArrayList<String[]> fieldValues = new ArrayList<String[]>();

		String[] allowed_vals = new String[allowed_var.getDomain().size().intValue() + 2];
		allowed_vals[0] = ALLOWED_VAR;
		allowed_vals[1] = allowed_var.getName();
		for (int i = 2; i < allowed_vals.length; i++) {
			allowed_vals[i] = Env.stringer.elementName(allowed_var.getDomain(), new BigInteger("" + (i - 2)));
		}
		fieldValues.add(allowed_vals);

		String[] switch_vals = new String[switch_var.getDomain().size().intValue() + 2];
		switch_vals[0] = SWITCH_VAR;
		switch_vals[1] = switch_var.getName();
		for (int i = 2; i < switch_vals.length; i++) {
			switch_vals[i] = Env.stringer.elementName(switch_var.getDomain(), new BigInteger("" + (i - 2)));
		}
		fieldValues.add(switch_vals);

		oos.writeObject(fieldValues);
		oos.close();

		this.vars.unionWith(Env.getVar(SWITCH_VAR).getDomain().set());
		this.vars.unionWith(Env.getVar(ALLOWED_VAR).getDomain().set());

		bridgeVars = Env.getEmptySet();
		bridgeVars.unionWith(Env.getVar(SWITCH_VAR).getDomain().set());
		bridgeVars.unionWith(Env.getVar(ALLOWED_VAR).getDomain().set());
	}

	public void switchController(Controller newController) throws IllegalStateException, IOException {
		System.out.println("Switches controller after " + String.valueOf(this.stepCount) + " bridge steps.");
		addTestData(String.valueOf(this.bridgeCount) + "," + String.valueOf(this.stepCount));
		if (currentState == null) {
			throw new IllegalStateException("Controller has not been initialized.");
		}

		Env.saveBDD(this.outFolder + CURRENT, this.currentState, true);

		Env.resetEnv();

		sysVars = new HashMap<>();
		envVars = new HashMap<>();
	    SaveLoadWithDomains.loadStructureAndDomains(this.outFolder + SaveLoadWithDomains.VARS_FILE, sysVars, envVars);

	    sysVars.entrySet().removeIf(var -> var.getKey().startsWith("util_"));
	    envVars.entrySet().removeIf(var -> var.getKey().startsWith("util_"));

		this.controller = newController;
		this.controller.load(this.outFolder, this.name, sysVars, envVars);

		System.out.println("New controller is loaded.");
		
		if (this.reordering) {
			Env.enableReorder();
		} else {
			Env.disableReorder();
		}
		
		BDD sysTrans = this.controller.transitions().id();
		Env.saveBDD(this.outFolder + SYS_TRANS, sysTrans, true);
		sysTrans.free();

		SaveLoadWithDomains.loadStructureAndDomains(this.outFolder + BRIDGE_VARS, null, null);

		BDD current = Env.loadBDD(this.outFolder + CURRENT);
		System.out.println(Env.toNiceSignleLineString(current));
		
		File f = new File(this.outFolder + CURRENT);
		if (f.exists()) {
			f.delete();
		}

		this.bridgeVars.free();
		this.bridgeVars = Env.getEmptySet();
		this.bridgeVars.unionWith(Env.getVar(SWITCH_VAR).getDomain().set());
		this.bridgeVars.unionWith(Env.getVar(ALLOWED_VAR).getDomain().set());
		this.currentState = current.exist(this.bridgeVars);

		controller.init(this.currentState);

		this.vars.free();
		this.vars = Env.getEmptySet();
		for (String sysVar : sysVars.keySet()) {
			this.vars.unionWith(Env.getVar(sysVar).getDomain().set());
		}
		for (String envVar : envVars.keySet()) {
			this.vars.unionWith(Env.getVar(envVar).getDomain().set());
		}
		this.vars.unionWith(Env.getVar(SWITCH_VAR).getDomain().set());
		this.vars.unionWith(Env.getVar(ALLOWED_VAR).getDomain().set());

		this.curBridge = -1;
		this.stepCount = 0;

		this.switchLock = false;
		this.updateLock.unlock();
		
		System.out.println("Controller was updated successfully.");
	}

	public boolean isCurrentStateInRegion(String regionPath) throws IOException {
		this.updateLock.lock();
		BDD region = Env.loadBDD(regionPath);
		
		BDD switch_false = Env.getBDDValue(SWITCH_VAR , "false");
		BDD allowed_false = Env.getBDDValue(ALLOWED_VAR, "false");
		BDD newCurrentState = this.currentState.exist(bridgeVars).and(switch_false).and(allowed_false);
		
		boolean ret = !((region.and(newCurrentState)).isZero());
		if (!ret) {
			this.updateLock.unlock();
		}
		
		region.free();

		return ret;
	}

	public void enableBridge() throws IOException {
		System.out.println("Enabling bridge...");
		this.bridgeTrans = Env.loadBDD(this.bridgeFolder + "trans.bdd");
		this.bridge = true;
		printControllerLog("bridge");
		this.curBridge = this.bridgeCount - 1;

		BDD switch_false = Env.getBDDValue(SWITCH_VAR , "false");
		BDD allowed_false = Env.getBDDValue(ALLOWED_VAR, "false");
		this.currentState = this.currentState.exist(bridgeVars).and(switch_false).and(allowed_false);
		switch_false.free();
		allowed_false.free();

		System.out.println("Bridge enabled.");
		lockSwitch();
		this.updateLock.unlock();
	}

	private void updateBridgeState(Map<String, String> inputs) throws IllegalStateException, IllegalArgumentException, IOException {
		this.stepCount += 1;
		System.out.println("Bridge step!");

		BDD currAndTrans = this.bridgeTrans.id();
		currAndTrans.andWith(this.currentState.id());

		if (currAndTrans.isZero()) {
			throw new IllegalStateException("The environment is in a deadlock state. There is no possible safe input for the environment");
		}

		BDD inputsBDD = getInputsBDD(inputs);
		currAndTrans.andWith(Env.prime(inputsBDD));

		if (currAndTrans.isZero()) {
			throw new IllegalArgumentException("The inputs are a safety violation for the environment");
		}
		
		System.out.println("Can step");

		int low = 0;
		int high = this.curBridge;
		int curStep;
		BDD curBridgeBDD = Env.FALSE();
		BDD stepCurrAndTrans = Env.FALSE();
		BDD curNextStates = Env.FALSE();
		BDD primedNextStates = Env.FALSE();
		int lowestRank = this.curBridge - 1;

		BDD nextStates = Env.FALSE();

		while (low <= high) {
			System.out.println("Binary iteration");
			curStep = (low + high) / 2;

			curBridgeBDD = Env.loadBDD(this.bridgeFolder + "bridge" + String.valueOf(curStep) + ".bdd");
			System.out.println("Loaded " + "bridge" + String.valueOf(curStep) + ".bdd");
			stepCurrAndTrans = currAndTrans.id();
			stepCurrAndTrans.andWith(Env.prime(curBridgeBDD));

			primedNextStates = stepCurrAndTrans.exist(Env.globalUnprimeVars());
			curNextStates = Env.unprime(primedNextStates);

			if (!curNextStates.isZero()) {
				System.out.println("Can jump to " + "bridge" + String.valueOf(curStep) + ".bdd");
				lowestRank = curStep;
				nextStates = curNextStates;
				high = curStep - 1;
			}
			else {
				System.out.println("Can't jump to " + "bridge" + String.valueOf(curStep) + ".bdd");
				low = curStep + 1;
			}
			stepCurrAndTrans.free();
		}

		this.curBridge = lowestRank;

		if (this.curBridge == 0) {
			System.out.println("Finished bridge");
			this.switchLock = false;
			this.bridge = false;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			printControllerLog("------------------------------------------------------------------------");
		}

		this.currentState.free();
		this.currentState = Env.randomSat(nextStates, vars);

		nextStates.free();
		inputsBDD.free();
		currAndTrans.free();
		curBridgeBDD.free();
		primedNextStates.free();
	}

	public String getOutDir() {
		return this.outFolder;
	}

	public String getBridgeDir() {
		return this.bridgeFolder;
	}

	public void setBridgeCount(int bridgeCount) {
		this.bridgeCount = bridgeCount;
	}

	public void lockSwitch() {
		while (this.switchLock) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.switchLock = true;
	}

	public void updateState(Map<String, String> inputs) throws IllegalStateException, IllegalArgumentException {
		this.updateLock.lock();

		if (this.bridge) {
			try {
				updateBridgeState(inputs);
				System.out.println(Env.toNiceSignleLineString(this.currentState));
			} catch (IOException e) {
				printUpdateError("Failed to update bridge state");
			}
		}
		else {
			super.updateState(inputs);
		}

		this.updateLock.unlock();
	}

	public void free() {
		this.server.terminate();
		try {
			this.server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.free();
	}
}
