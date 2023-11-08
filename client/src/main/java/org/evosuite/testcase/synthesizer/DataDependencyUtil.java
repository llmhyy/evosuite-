package org.evosuite.testcase.synthesizer;

import org.evosuite.Properties;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.GraphPool;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.instrumentation.InstrumentingClassLoader;
import org.evosuite.runtime.System;
import org.evosuite.setup.DependencyAnalysis;
import org.evosuite.utils.MethodUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.*;
import java.util.*;

public class DataDependencyUtil {
	/**
	 * Output is the valid parameter position of most top method call
	 * 
	 * @param setterInstruction
	 * @param className
	 * @param methodName
	 * @param callList
	 * @return
	 */
	public static Set<Integer> checkValidParameterPositions(BytecodeInstruction setterInstruction, String className,
			String methodName, List<BytecodeInstruction> callList) {
		Collections.reverse(callList);

		// only check for current method
		if (callList.isEmpty()) {
			Set<BytecodeInstruction> paramInstructions = checkCurrentParamInstructions(setterInstruction,
					new HashSet<>());
			System.currentTimeMillis();
			Set<Integer> paramPositions = checkSetterParamPositions(paramInstructions);
			return paramPositions;
		}

		Set<BytecodeInstruction> paramInstructions = analyzingCascadingCall(
				new HashSet<>(Arrays.asList(setterInstruction)), callList, 0, new HashSet<>());
		Set<Integer> paramPositions = checkSetterParamPositions(paramInstructions);
		return paramPositions;
	}
	
	
	private static Set<BytecodeInstruction> analyzingCascadingCall(Set<BytecodeInstruction> insns,
			List<BytecodeInstruction> callList, int index, Set<BytecodeInstruction> topParamInsns) {
		BytecodeInstruction call = callList.get(index);
		for (BytecodeInstruction ins : insns) {
			Set<BytecodeInstruction> paramInsns = checkCurrentParamInstructions(ins, new HashSet<>());
			if (paramInsns.isEmpty()) {
				return paramInsns;
			}
			boolean isValidParameter = checkParamValidation(call, paramInsns, ins);
			System.currentTimeMillis();
			if (!isValidParameter) {
				return new HashSet<>();
			}
			Set<BytecodeInstruction> newParamInsns = searchForNewParameterInstruction(call, paramInsns);
			if (index == callList.size() - 1) {
				topParamInsns.addAll(newParamInsns);
			} else {
				analyzingCascadingCall(newParamInsns, callList, index + 1, topParamInsns);
			}
		}
		return topParamInsns;
	}
	
	/**
	 * search for new load instruction when analyzing next layer of method
	 * invocation
	 * 
	 * @param call
	 * @param paramPos
	 * @return
	 */
	private static Set<BytecodeInstruction> searchForNewParameterInstruction(BytecodeInstruction call,
			Set<BytecodeInstruction> paramInsns) {
		Set<BytecodeInstruction> newParamInsns = new HashSet<>();
		Set<Integer> paramPosSet = checkSetterParamPositions(paramInsns);
		Set<BytecodeInstruction> newParams = new HashSet<>();
		for (Integer pos : paramPosSet) {
			if (call.getASMNode().getOpcode() == Opcodes.INVOKESTATIC) {
				BytecodeInstruction defIns = call.getSourceOfStackInstruction(call.getOperandNum() - pos - 1);
				newParams = checkCurrentParamInstructions(defIns, new HashSet<>());
			} else {
				BytecodeInstruction defIns = call.getSourceOfStackInstruction(call.getOperandNum() - pos - 2);
				newParams = checkCurrentParamInstructions(defIns, new HashSet<>());
			}
			if (newParams != null) {
				newParamInsns.addAll(newParams);
			}
		}
		return newParamInsns;
	}

	/**
	 * return localVariableUse instruction for parameter
	 * 
	 * @param descIns
	 * @param paramInstructionSet
	 * @return
	 */
	private static Set<BytecodeInstruction> checkCurrentParamInstructions(BytecodeInstruction descIns,
			Set<BytecodeInstruction> paramInstructionSet) {
		if (descIns.isLocalVariableUse()) {
			if (descIns.isParameter()) {
				// position here similar to list index (i.e., starts from 0)
//				int pos = checkSetterParamPos(defIns);
//				paramInstructionSet.add(new Integer(pos));
				paramInstructionSet.add(descIns);
			}
		}
		for (int i = 0; i < descIns.getOperandNum(); i++) {
			BytecodeInstruction defIns = descIns.getSourceOfStackInstruction(i);
			if (defIns == null) {
				return paramInstructionSet;
			}
			if (defIns.isLocalVariableUse()) {
				if (defIns.isParameter()) {
					// position here similar to list index (i.e., starts from 0)
//					int pos = checkSetterParamPos(defIns);
//					paramInstructionSet.add(new Integer(pos));
					paramInstructionSet.add(defIns);
				}
			}
			checkCurrentParamInstructions(defIns, paramInstructionSet);
		}
		return paramInstructionSet;
	}
	
	private static boolean checkParamValidation(BytecodeInstruction call, Set<BytecodeInstruction> paramInsns,
			BytecodeInstruction ins) {
		if (call.getCalledMethodsArgumentCount() == 0) {
			return false;
		}
		if (!call.getCalledMethod().equals(ins.getMethodName())){
			return false;
		}
		
		String class1 = call.getCalledMethodsClass();
		String class2 = ins.getClassName();
		
		InstrumentingClassLoader classLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();
		Class<?> c1 = null;
		Class<?> c2 = null;
		
		try {
			 c1 = classLoader.loadClass(class1);
			 c2 = classLoader.loadClass(class2);
		} catch (ClassNotFoundException e) {
//			e.printStackTrace();
		}
		
		if(c1 != null && c2 != null) {
			if(!c2.isAssignableFrom(c1)) {
				return false;
			}
		}
		else {
			if(!class1.equals(class2)) {
				return false;
			}
		}
		

		Set<Integer> paramPositions = checkSetterParamPositions(paramInsns);
		for (Integer pos : paramPositions) {
			if (pos > call.getCalledMethodsArgumentCount() - 1) {
				return false;
			}
		}
		return true;
	}
	
	private static Set<Integer> checkSetterParamPositions(Set<BytecodeInstruction> paramInsns) {
		Set<Integer> paramPositions = new HashSet<>();
		for (BytecodeInstruction paramInstruction : paramInsns) {
			int parameterPosition = paramInstruction.getParameterPosition();
			System.currentTimeMillis();
			if(parameterPosition != -1){
				paramPositions.add(parameterPosition);				
			}
		}
		return paramPositions;
	}
	
	
	/**
	 * check whether a method can return the field.
	 * @param method
	 * @param field
	 * @return
	 */
	public static boolean isFieldGetter(Method method, Field field) {
		int opcode = Modifier.isStatic(field.getModifiers()) ? Opcodes.GETSTATIC : Opcodes.GETFIELD;

		if (!method.getReturnType().getCanonicalName().equals(field.getType().getCanonicalName())) {
			return false;
		}

		ActualControlFlowGraph cfg = GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT())
				.getActualCFG(method.getDeclaringClass().getCanonicalName(),
						method.getName() + MethodUtil.getSignature(method));
		if (cfg == null) {
			return false;
		}
		
		return checkValidGetter(field, opcode, cfg);
	}
	
	private static boolean checkValidGetter(Field field, int opcode, ActualControlFlowGraph cfg) {
		for (BytecodeInstruction exit : cfg.getExitPoints()) {
			if (exit.isReturn()) {
				BytecodeInstruction insn = exit.getSourceOfStackInstruction(0);
				if (insn.getASMNode().getOpcode() == opcode) {
					String fieldName = getFieldName(insn);
					if (fieldName.equals(field.getName())) {
						return true;
					}
				} else if (insn.isMethodCall()) {
					ActualControlFlowGraph calledCfg = insn.getCalledActualCFG();
					if (calledCfg == null) {
						calledCfg = MethodUtil.registerMethod(insn.getCalledMethodsClass(), insn.getCalledMethod());
					}
					if (calledCfg != null) {
						return checkValidGetter(field, opcode, calledCfg);
					}
				}
			}
		}
		
		return false;
	}
	
	public static String getFieldName(BytecodeInstruction ins) {
		if (ins.getASMNode().getType() == AbstractInsnNode.FIELD_INSN) {
			FieldInsnNode fNode = (FieldInsnNode) ins.getASMNode();
			return fNode.name;
		}

		return null;
	}
	
//	public static List<ValueSettings> findSetterInfo(Field field, Class<?> targetClass, 
//			Executable m, String signature) {
//		/**
//		 * initialization
//		 */
//		
//		/**
//		 * get the field setting map
//		 */
//		List<ValueSettings> fieldValueSettings = new ArrayList<>();
//		DataDependencyUtil.analyzeFieldSetter(targetClass.getCanonicalName(), signature,
//				field, Properties.FIELD_SETTER_SEARCH_DEPTH, new ArrayList<>(), fieldValueSettings);
//		
////		System.currentTimeMillis();
////		
////		Set<Integer> releventPrams = new HashSet<>();
////		for (Entry<BytecodeInstruction, List<BytecodeInstruction>> entry : fieldSetterMap.entrySet()) {
////			BytecodeInstruction setterIns = entry.getKey();
////			List<BytecodeInstruction> callList = entry.getValue();
////			Set<Integer> validParamPos = DataDependencyUtil.checkValidParameterPositions(setterIns, 
////					targetClass.getCanonicalName(), signature, callList);
////			releventPrams.addAll(validParamPos);
////		}
////		
////		if(!fieldValueSettings.isEmpty()){
////			fieldSettingMethods.add(m);
////			difficultyList.add(fieldSetterMap);
////			validParams.add(releventPrams);
////		}
//	}
	
	@SuppressWarnings("rawtypes")
	public static LinkedHashMap<Executable, List<ValueSettings>> searchForPotentialSettersInClass(Field field, String targetClassName) throws ClassNotFoundException {
		LinkedHashMap<Executable, List<ValueSettings>> map = new LinkedHashMap<>();
		Class<?> targetClass = TestGenerationContext.getInstance().getClassLoaderForSUT()
				.loadClass(targetClassName);
		
		for(Method m: targetClass.getMethods()){
			String signature = m.getName() + ReflectionUtil.getSignature(m);
			List<ValueSettings> fieldValueSettings = new ArrayList<>();
			DataDependencyUtil.analyzeFieldSetter(targetClass.getCanonicalName(), signature,
					field, Properties.FIELD_SETTER_SEARCH_DEPTH, new ArrayList<>(), fieldValueSettings);
			if(!fieldValueSettings.isEmpty()) {
				map.put(m, fieldValueSettings);
			}
		}
		
		for(Constructor c: targetClass.getConstructors()){
			String signature = "<init>" + ReflectionUtil.getSignature(c);
			List<ValueSettings> fieldValueSettings = new ArrayList<>();
			DataDependencyUtil.analyzeFieldSetter(targetClass.getCanonicalName(), signature,
					field, Properties.FIELD_SETTER_SEARCH_DEPTH, new ArrayList<>(), fieldValueSettings);
			if(!fieldValueSettings.isEmpty()) {
				map.put(c, fieldValueSettings);
			}
		}
		
		return map;
	}
	
	public static void analyzeFieldSetter(String className, String methodName,
			Field field, int depth, List<BytecodeInstruction> cascadingCallRelations,
			List<ValueSettings> setterList) {
		analyzeFieldSetter(className, methodName, className, methodName, field, depth, cascadingCallRelations, setterList);
	}
	
	/**
	 * 
	 * return a map from a field-setting instruction to a set of calls like <m1(), m2(), ..., mk()>,
	 * in which, 
	 * m1() is the method used in test;
	 * mk() is the method setting the field.
	 *  
	 * 
	 * @param className
	 * @param methodName
	 * @param field
	 * @param depth
	 * @param cascadingCallRelations
	 * @param setterList
	 * @return
	 */
	private static void analyzeFieldSetter(String targetClass, String targetSig, String className, String methodName,
			Field field, int depth, List<BytecodeInstruction> cascadingCallRelations,
			List<ValueSettings> setterList) {
		List<BytecodeInstruction> insList = null;

//		MockFramework.disable();
		System.currentTimeMillis();
		try{
			ClassLoader classLoader = TestGenerationContext.getInstance().getClassLoaderForSUT();
			insList = BytecodeInstructionPool.getInstance(classLoader).getAllInstructionsAtMethod(className, methodName);
			
			if(insList == null) {
				try {
					GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).registerClass(className);
				} catch (Exception e) {
					System.currentTimeMillis();
				}
//				RawControlFlowGraph cfg = GraphPool.getInstance(classLoader).getRawCFG(className, methodName);

				insList = BytecodeInstructionPool.getInstance(classLoader).getAllInstructionsAtMethod(className, methodName);
				if(insList == null) {
					Class<?> c = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(className);
					Class<?> superClass = c.getSuperclass();
					
					while(superClass != null) {
						String superClassName = superClass.getCanonicalName();
//						GraphPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT()).registerClass(superClassName);
						insList = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT())
								.getAllInstructionsAtMethod(superClassName, methodName);	
						
						if(insList != null) {
							break;
						}
						else {
							superClass = superClass.getSuperclass();
						}
					}
				}
				
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}

		String opcode = Modifier.isStatic(field.getModifiers()) ? "PUTSTATIC" : "PUTFIELD";
		if (insList != null) {
			for (BytecodeInstruction ins : insList) {
				if(isCollectionType(field.getType())) {
					if(isCallElementModification(ins, field)) {
						ValueSettings settings = new ValueSettings(ins, cascadingCallRelations, targetClass, targetSig);
						setterList.add(settings);
//						setterMap.put(ins, new ArrayList<>(cascadingCallRelations));
					}
				}
				
				
				if (ins.getASMNodeString().contains(opcode)) {
					AbstractInsnNode node = ins.getASMNode();
					if (node instanceof FieldInsnNode) {
						FieldInsnNode fNode = (FieldInsnNode) node;
						if (fNode.name.equals(field.getName())) {
							ValueSettings settings = new ValueSettings(ins, cascadingCallRelations, targetClass, targetSig);
							setterList.add(settings);
//							setterList.put(ins, new ArrayList<>(cascadingCallRelations));
						}
					}
				} 
				else if (ins.getASMNode() instanceof MethodInsnNode) {
					if (depth > 0) {
						MethodInsnNode mNode = (MethodInsnNode) (ins.getASMNode());

						String calledClass = mNode.owner;
						calledClass = calledClass.replace("/", ".");
						/**
						 * FIXME: we only analyze the callee method in the same class, but we
						 * need to consider the invocation in other class.
						 */
						if (calledClass.equals(className)) {
							String calledMethodName = mNode.name + mNode.desc;

							String confirmedCalledClass = confirmClassNameInParentClass(calledClass, mNode);
							if (calledMethodName != null) {
								cascadingCallRelations.add(ins);
								analyzeFieldSetter(targetClass, targetSig, confirmedCalledClass, calledMethodName, field, depth - 1,
										cascadingCallRelations, setterList);
							}
						} else {
							System.currentTimeMillis();
						}
					} 
				}
			}
		}
		cascadingCallRelations.clear();

//		return setterList;

	}
	
	/**
	 * check the call to elements.
	 * @param ins
	 * @return
	 */
	private static boolean isCallElementModification(BytecodeInstruction ins, Field field) {
		// TODO aaron
//		java.lang.System.out.println(ins.getMethodName());
//		java.lang.System.out.println(ins.getASMNodeString());
		
		if(!ins.isMethodCall()) return false;
		
		boolean useCorrectField = false;
		List<BytecodeInstruction> operands = ins.getOperands();
		for(BytecodeInstruction instruction: operands) {
			if(instruction.isFieldUse()) {
				AbstractInsnNode asmNode = instruction.getASMNode();
				if(asmNode instanceof FieldInsnNode) {
					String fieldName = ((FieldInsnNode)asmNode).name;
					if(fieldName != null && fieldName.equals(field.getName())) {
						useCorrectField = true;
					}
					
				}
				
			}
		}
		
		if(!useCorrectField) {
			return false;
		} 
		
		// Check if instruction invokes another method
		AbstractInsnNode asmNode = ins.getASMNode();
		if (asmNode instanceof MethodInsnNode && asmNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
			MethodInsnNode methodNode = (MethodInsnNode) asmNode; 
			
			// Check if invoked method is a method from Collection class
			Class<?> methodClassType;
			try {
				String typeString = methodNode.owner.replace('/', '.');
				methodClassType = TestGenerationContext.getInstance().getClassLoaderForSUT().loadClass(typeString);
//				methodClassType = Class.forName();
				if (isCollectionType(methodClassType)) {
					
					// Check if invoked method modifies element
					Set<String> collectionModifyMethodNames = new HashSet<>(Arrays.asList(
							"add", 
							"addAll", 
							"addElement",
							"addFirst",
							"addLast",
							"clear",
							"ensureCapacity",
							"offer",
							"offerFirst",
							"offerLast",
							"poll",
							"pollFirst",
							"pollLast",
							"pop",
							"push",
							"remove",
							"removeAll",
							"removeElement",
							"removeElementAt",
							"removeFirst",
							"removeFirstOccurrence",
							"removeIf",
							"removeLast",
							"removeLastOccurrence",
							"removeRange",
							"replaceAll",
							"retainAll",
							"set",
							"setElementAt",
							"setSize",
							"sort",
							"trimToSize"));
					
//					java.lang.System.out.println(collectionModifyMethodNames.contains(methodNode.name));
					return collectionModifyMethodNames.contains(methodNode.name);
					
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
	
	/**
	 * all the java classes from java.util.List/Collection
	 * @param type
	 * @return
	 */
	private static boolean isCollectionType(Class<?> type) {
		// TODO aaron
		return Collection.class.isAssignableFrom(type);
	}

	private static String confirmClassNameInParentClass(String calledClass, MethodInsnNode mNode) {
		ClassNode classNode = DependencyAnalysis.getClassNode(calledClass);
		List<String> superClassList = DependencyAnalysis.getInheritanceTree().getOrderedSuperclasses(calledClass);
		for (String superClass : superClassList) {
			ClassNode parentClassNode = DependencyAnalysis.getClassNode(superClass);
			if (!containMethod(classNode, mNode) && containMethod(parentClassNode, mNode)) {
				return superClass;
			}
		}

		System.currentTimeMillis();

		return calledClass;
	}
	
	private static boolean containMethod(ClassNode classNode, MethodInsnNode mNode) {

//		if(!mNode.owner.equals(classNode.name)) {
//			return false;
//		}

		for (MethodNode methodNode : classNode.methods) {
			String methodNodeName = methodNode.name + methodNode.desc;
			String methodInsName = mNode.name + mNode.desc;
			if (methodNodeName.equals(methodInsName)) {
				return true;
			}
		}

		return false;
	}
}
