var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var LdcInsnNode = Java.type('org.objectweb.asm.tree.LdcInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode');
var FrameNode = Java.type('org.objectweb.asm.tree.FrameNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var IntInsnNode = Java.type('org.objectweb.asm.tree.IntInsnNode');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var Type = Java.type('org.objectweb.asm.Type');
var Label = Java.type('org.objectweb.asm.Label');

/**
 * Creates a utility class used for storing instructions.
 */
function newInsnList() {
    var insn = [];
    return {
        /**
         * Pushes an instruction at the end of the list.
         * @param instr the instruction to push
         */
        'push': function (instr) {
            insn.push(instr)
        },

        'visitLabel': function (label) {
            insn.push(getLabelInsn(label));
        },
        'visitFieldInsn': function (opcode, owner, name, desc) {
            insn.push(new FieldInsnNode(opcode, owner, name, desc));
        },
        'visitMethodInsn': function (opcode, owner, name, desc, isInterface) {
            insn.push(new MethodInsnNode(opcode, owner, name, desc, isInterface));
        },
        'visitLdcInsn': function (val) {
            insn.push(new LdcInsnNode(val));
        },
        'visitInsn': function (opcode) {
            insn.push(new InsnNode(opcode));
        },
        'visitJumpInsn': function (opcode, label) {
            insn.push(new JumpInsnNode(opcode, getLabelInsn(label)));
        },
        'visitTypeInsn': function (opcode, type) {
            insn.push(new TypeInsnNode(opcode, type));
        },
        'visitFrame': function (type, numLocal, numStack) {
            insn.push(new FrameNode(type, numLocal, null, numStack, null));
        },
        'visitVarInsn': function (opcode, varType) {
            insn.push(new VarInsnNode(opcode, varType));
        },
        'visitIntInsn': function (opcode, operand) {
            insn.push(new IntInsnNode(opcode, operand));
        },

        // Util methods

        /**
         * Adds an ATHROW instruction for throwing (an already present on the stack) Exception of the specified type,
         * which was created using a constructor with the '(Ljava/lang/String;)V' desc.
         * @param type the type of the exception
         */
        'throwWithMessage': function (type) {
            this.visitMethodInsn(Opcodes.INVOKESPECIAL, type, "<init>", "(Ljava/lang/String;)V", false);
            this.visitInsn(Opcodes.ATHROW);
        },

        /**
         * Creates a new instruction list from the instructions added.
         * @returns the instruction list
         */
        'get': function () {
            return ASMAPI.listOf(insn);
        }
    }
}

/**
 * Inserts instructions at the head of the method
 * @param method the method to insert instructions into
 * @param instructions the instructions to insert
 */
function insertHeadInstructions(method, instructions) {
    if (method.name == '<init>') {
        // We're in a constructor so we need to insert after the super call
        var sup = findSuperCtorCall(method);
        if (sup != null) {
            method.instructions.insert(sup, instructions);
            return;
        }
    }
    method.instructions.insert(instructions);
}

/**
 * Finds the 'super()' constructor call in a method.
 * @param method the method to check
 * @returns {null|MethodInsnNode} the super insn, otherwise null
 */
function findSuperCtorCall(method) {
    for (var i = 0; i < method.instructions.size(); i++) {
        var ain = method.instructions.get(i);
        if (ain instanceof MethodInsnNode) {
            if (ain.name == '<init>')
                return ain;
        }
    }
    return null;
}

function getLabelInsn(label) {
    if (!label.info)
        label.info = new LabelNode();
    return label.info
}

function getType(type) {
    return Type.getType(type);
}

function hasModifier(access, mod) {
    return (access & mod) !== 0;
}

function getAccess(access) {
    if (hasModifier(access, Opcodes.ACC_PRIVATE))
        return Opcodes.ACC_PRIVATE;
    else if (hasModifier(access, Opcodes.ACC_PUBLIC))
        return Opcodes.ACC_PUBLIC;
    else if (hasModifier(access, Opcodes.ACC_PROTECTED))
        return Opcodes.ACC_PROTECTED;
    return 0;
}

function modifyAccess(access, newAccess) {
    access = removeModifier(access, Opcodes.ACC_PRIVATE);
    access = removeModifier(access, Opcodes.ACC_PUBLIC);
    access = removeModifier(access, Opcodes.ACC_PROTECTED);
    return access & newAccess;
}

function removeModifier(access, toRemove) {
    if (hasModifier(access, toRemove))
        return access & (~toRemove);
    return access;
}

/**
 * Finds or creates a method.
 * @param classNode the node of the class to check in.
 * @param name the name of the method to find
 * @param desc the desc of the method to find
 * @param modifiers the modifiers of the new method, if it needs to be created
 * @returns {MethodNode} the found or created method
 */
function findOrCreateMethod(classNode, name, desc, modifiers) {
    var type = getType(desc);
    var method = findMethodNode(classNode, name, desc);
    if (!method) {
        method = classNode.visitMethod(modifiers, name, desc, null, null);
        method.visitCode();
        method.visitInsn(type.getOpcode(Opcodes.IRETURN));
        method.visitMaxs(0, 0);
        method.visitEnd();
    }
    return method;
}

function findMethodNode(classNode, name, desc) {
    for (var x = 0; x < classNode.methods.length; x++) {
        var method = classNode.methods[x];
        if (method.name == name && method.desc == desc) {
            return method;
        }
    }
    return null;
}

function getInternalName(clazz) {
    return clazz.replaceAll('\\.', '/')
}

function insertDelegateCall(node, ownerName, params, isStatic, isInterface, returnType, delegate) {
    ownerName = getInternalName(ownerName);
    var isVoid = returnType == 'V';
    var label0 = new Label();
    node.visitLabel(label0);
    var localIndex = 0;
    if (!isStatic) {
        localIndex++;
        node.visitVarInsn(Opcodes.ALOAD, 0);
    }
    var localIndexes = []
    for (var index = 0; index < params.length; index++) {
        localIndex += index == 0 ? 0 : getType(params[index - 1]).getSize()
        localIndexes.push(localIndex)
        node.visitVarInsn(getType(params[index]).getOpcode(Opcodes.ILOAD), localIndex);
    }
    node.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, isStatic ? getInternalName(delegate.clazz) : ownerName,
        delegate.name, delegate.desc, isInterface);
    node.visitInsn(isVoid ? Opcodes.RETURN : getType(returnType).getOpcode(Opcodes.IRETURN));
    var label1 = new Label();
    node.visitLabel(label1);
    if (!isStatic) node.visitLocalVariable("this", 'L' + ownerName + ';', null, label0, label1, 0);
    for (var i = 0; i < params.length; i++)
        node.visitLocalVariable("arg" + i, params[i], null, label0, label1, localIndexes[i]);
    var max = localIndex + (isStatic ? 0 : 1) + (isVoid ? 0 : getType(returnType).getSize());
    node.visitMaxs(max, max);
}