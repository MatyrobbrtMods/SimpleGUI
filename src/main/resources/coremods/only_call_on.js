var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var Label = Java.type('org.objectweb.asm.Label');

function initializeCoreMod() {
    ASMAPI.loadFile('coremods/utils.js');

    var data = ASMAPI.loadData('coremods/call_only_on.json');
    var ret = {};
    for (var name in data) {
        var dt = data[name];
        if (!dt.logical && dt.method.name == '<clinit>') {
            clinit(ret, dt.clazz, dt.size);
        } else {
            transform(ret, dt.clazz, dt.method.name, dt.method.desc, dt.side, dt.logical);
        }
    }
    return ret;
}

function clinit(ret, className, dist) {
    ret[className + "#<clinit>"] = {
        'target': {
            'type': 'CLASS',
            'name': className
        },
        'transformer': function (node) {
            var clinit = findOrCreateMethod(node, '<clinit>', '()V', Opcodes.ACC_STATIC);
            doTransform(clinit, className, '<clinit>', '()V', dist, false);
            return node;
        }
    }
}

function transform(ret, className, method, desc, dist, logical) {
    ret[className + "#" + method + desc] = {
        'target': {
            'type': 'METHOD',
            'class': className,
            'methodName': method,
            'methodDesc': desc
        },
        'transformer': function (node) {
            doTransform(node, className, method, desc, dist, logical)
            return node;
        }
    }
}

function doTransform(node, className, method, desc, dist, logical) {
    var actualMethod = {
        'clazz': className,
        'name': method,
        'desc': desc
    };
    if (logical) {
        insertHeadInstructions(node, createLogicalCheck(dist.toLocaleUpperCase(), actualMethod).get());
    } else {
        insertHeadInstructions(node, createPhysicalCheck(dist.toLocaleUpperCase(), actualMethod).get());
    }
    node.maxStack += 5;
}

function createPhysicalCheck(side, method) {
    var insn = newInsnList();
    if (side == 'SERVER')
        side = 'DEDICATED_SERVER';
    var label0 = new Label();
    insn.visitLabel(label0);
    insn.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/loading/FMLLoader", "getDist", "()Lnet/minecraftforge/api/distmarker/Dist;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/api/distmarker/Dist", "name", "()Ljava/lang/String;", false);
    insn.visitLdcInsn(side);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    var label1 = new Label();
    insn.visitJumpInsn(Opcodes.IFNE, label1);
    var label2 = new Label();
    insn.visitLabel(label2);
    insn.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalCallerException");
    insn.visitInsn(Opcodes.DUP);
    var label3 = new Label();
    insn.visitLabel(label3);
    insn.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
    insn.visitInsn(Opcodes.DUP);
    insn.visitLdcInsn("Attempted to call method '" + method.clazz + "#" + method.name + method.desc + "' on invalid dist: ");
    insn.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
    insn.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/loading/FMLLoader", "getDist", "()Lnet/minecraftforge/api/distmarker/Dist;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    var label4 = new Label();
    insn.visitLabel(label4);
    insn.throwWithMessage("java/lang/IllegalCallerException");
    insn.visitLabel(label1);
    insn.visitFrame(Opcodes.F_SAME, 0, 0);
    return insn;
}

function createLogicalCheck(side, method) {
    var insn = newInsnList();
    var label0 = new Label();
    insn.visitLabel(label0);
    insn.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/util/thread/EffectiveSide", "get", "()Lnet/minecraftforge/fml/LogicalSide;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/fml/LogicalSide", "name", "()Ljava/lang/String;", false);
    insn.visitLdcInsn(side);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    var label1 = new Label();
    insn.visitJumpInsn(Opcodes.IFNE, label1);
    var label2 = new Label();
    insn.visitLabel(label2);
    insn.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalCallerException");
    insn.visitInsn(Opcodes.DUP);
    var label3 = new Label();
    insn.visitLabel(label3);
    insn.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
    insn.visitInsn(Opcodes.DUP);
    insn.visitLdcInsn("Attempted to call method '" + method.clazz + "#" + method.name + method.desc + "' on invalid logical side: ");
    insn.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
    insn.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraftforge/fml/util/thread/EffectiveSide", "get", "()Lnet/minecraftforge/fml/LogicalSide;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
    insn.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    var label4 = new Label();
    insn.visitLabel(label4);
    insn.throwWithMessage('java/lang/IllegalCallerException');
    insn.visitLabel(label1);
    insn.visitFrame(Opcodes.F_SAME, 0, 0);
    return insn;
}