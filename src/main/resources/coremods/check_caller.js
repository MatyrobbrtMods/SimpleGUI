var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var Label = Java.type('org.objectweb.asm.Label');

function initializeCoreMod() {
    ASMAPI.loadFile('coremods/utils.js');

    var data = ASMAPI.loadData('coremods/check_caller.json');
    var ret = {};
    for (var i in data) {
        var dt = data[i];
        var clazz = dt.clazz;
        withFields(ret, clazz, dt.methods);
        for (var x in dt.methods) {
            var method = dt.methods[x];
            if (method.name == '<clinit>') {
                transformClinit(ret, clazz, method.callers, method.blacklist, method.exception);
            } else {
                transformMethod(ret, clazz, method, x);
            }
        }
    }
    return ret;
}

function transformClinit(ret, clazz, callers, blacklist, exception) {
    ret[className + "#<clinit>()V"] = {
        'target': {
            'type': 'CLASS',
            'name': clazz
        },
        'transformer': function (node) {
            var cinit = findOrCreateMethod(node, '<clinit>', '()V', Opcodes.ACC_STATIC);
            var insn = newInsnList();
            var label0 = new Label();
            insn.visitLabel(label0);
            for (var i = 0; i < callers.length; i++)
                insn.visitLdcInsn(callers[i]);
            insn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;", true);
            addCheck(insn, blacklist, exception);
            insertHeadInstructions(cinit, insn.get());
            cinit.maxStack += 3;
            return node;
        }
    }
}

function computeFieldName(method, i) {
    var name = method.name;
    if (name == '<init>')
        name = 'init';
    return "ALLOWED_CALLERS_" + name + "_" + i;
}

function transformMethod(ret, className, method, i) {
    ret[className + "#" + method.name + method.desc] = {
        'target': {
            'type': 'METHOD',
            'class': className,
            'methodName': method.name,
            'methodDesc': method.desc
        },
        'transformer': function (node) {
            var internalName = className.replaceAll('\\.', '/');
            var fieldName = computeFieldName(method, i);
            var insn = newInsnList();
            var label0 = new Label();
            insn.visitLabel(label0);
            insn.visitFieldInsn(Opcodes.GETSTATIC, internalName, fieldName, "Ljava/util/List;");
            addCheck(insn, method.blacklist, method.exception);
            insertHeadInstructions(node, insn.get());
            node.maxStack += 3;
            return node;
        }
    }
}

function addCheck(insn, blacklist, exception) {
    var label1 = new Label();
    insn.visitLabel(label1);
    insn.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/StackWalker$Option", "RETAIN_CLASS_REFERENCE", "Ljava/lang/StackWalker$Option;");
    insn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/StackWalker", "getInstance", "(Ljava/lang/StackWalker$Option;)Ljava/lang/StackWalker;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StackWalker", "getCallerClass", "()Ljava/lang/Class;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
    insn.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "contains", "(Ljava/lang/Object;)Z", true);
    var label2 = new Label();
    insn.visitJumpInsn(blacklist ? Opcodes.IFEQ : Opcodes.IFNE, label2);
    var label3 = new Label();
    insn.visitLabel(label3);
    insn.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalCallerException");
    insn.visitInsn(Opcodes.DUP);
    insn.visitLdcInsn(exception);
    insn.throwWithMessage("java/lang/IllegalCallerException");
    insn.visitLabel(label2);
    insn.visitFrame(Opcodes.F_SAME, 0, 0);
}

function withFields(ret, className, fields) {
    ret[className + "_fields"] = {
        'target': {
            'type': 'CLASS',
            'name': className
        },
        'transformer': function (node) {
            var clinit = findOrCreateMethod(node, '<clinit>', '()V', Opcodes.ACC_STATIC);
            var stack = 0;
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                var fieldName = computeFieldName(field, i);
                var fieldVisitor = node.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, fieldName,
                    "Ljava/util/List;", "Ljava/util/List<Ljava/lang/String;>;", null);
                fieldVisitor.visitEnd();

                var insn = newInsnList();
                insn.push(getLabelInsn(new Label()));
                for (var z in field.callers) {
                    insn.visitLdcInsn(field.callers[z]);
                }
                insn.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/List", "of", "(Ljava/lang/Object;)Ljava/util/List;", true);
                insn.visitFieldInsn(Opcodes.PUTSTATIC, node.name, fieldName, "Ljava/util/List;");
                stack++;
                clinit.instructions.insert(insn.get());
            }
            clinit.maxStack += stack;
            return node;
        }
    }
}