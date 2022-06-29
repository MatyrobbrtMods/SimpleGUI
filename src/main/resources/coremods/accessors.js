var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');

// TODO method invokers
function initializeCoreMod() {
    ASMAPI.loadFile('coremods/utils.js');

    var data = ASMAPI.loadData('coremods/accessors.json');
    var ret = {};
    for (var name in data) {
        var dt = data[name];
        switch (dt.type.toLocaleLowerCase()) {
            case 'interface':
                interfaceTransform(ret, dt.target, dt.intermediary, dt.accessors);
                break;
            case 'abstract':
                abstractTransform(ret, dt.target, dt.intermediary, dt.accessors);
                break;
        }
    }
    return ret;
}

function interfaceTransform(ret, target, iface, accessors) {
    var transforms = []
    for (var i = 0; i < accessors.length; i++) {
        var accessor = accessors[i];
        var transformer = createInterfaceAccessor(ret, target, iface, accessor);
        if (transformer)
            transforms.push(transformer)
    }
    ret["interfaceAccessor_" + target + "|" + iface] = {
        'target': {
            'type': 'CLASS',
            'name': target
        },
        'transformer': function (node) {
            node.interfaces.add(getInternalName(iface));
            for (var x = 0; x < transforms.length; x++) {
                var method = transforms[x]();
                if (findMethodNode(node, method.name, method.desc)) {
                    ASMAPI.log('DEBUG', "Found accessor conflict: " + iface + "#" + method.name + method.desc);
                } else {
                    node.methods.add(method);
                }
            }
            return node;
        }
    };
}

function createInterfaceAccessor(ret, target, intermediary, accessor) {
    switch (accessor.type.toLocaleLowerCase()) {
        case 'field':
            if (accessor.static) {
                var getter = accessor.method.toLocaleLowerCase() == 'get';
                var desc = getter ? "()" + accessor.field.desc : "(" + accessor.field.desc + ")V";
                ret["redirectStaticIFACEField_" + intermediary + "#" + accessor.name + desc] = {
                    'target': {
                        'type': 'METHOD',
                        'class': intermediary,
                        'methodName': accessor.name,
                        'methodDesc': desc
                    },
                    'transformer': function (node) {
                        node.instructions.clear();
                        insertDelegateCall(node, intermediary, getter ? [] : [accessor.field.desc],
                            true, false, getter ? accessor.field.desc : "V", {
                                'name': accessor.name,
                                'desc': desc,
                                'clazz': target
                            });
                        return node;
                    }
                }
            }
            return createFieldAccessor(ret, target, intermediary, accessor);
        case 'method':
            if (accessor.static) {
                var desc = accessor.method.desc;
                ret["redirectStaticIFACEMethod_" + intermediary + "#" + accessor.name + desc] = {
                    'target': {
                        'type': 'METHOD',
                        'class': intermediary,
                        'methodName': accessor.name,
                        'methodDesc': desc
                    },
                    'transformer': function (node) {
                        node.instructions.clear();
                        insertDelegateCall(node, intermediary, accessor.method.params,
                            true, false, accessor.method.returnType, {
                                'name': accessor.name,
                                'desc': desc,
                                'clazz': target
                            });
                        return node;
                    }
                }
            }
            return createMethodAccessor(ret, target, intermediary, accessor);
    }
}

function abstractTransform(ret, target, subclass, accessors) {
    var transforms = []
    for (var i = 0; i < accessors.length; i++) {
        var accessor = accessors[i];
        var transformer = createAbstractTransform(ret, target, subclass, accessor);
        if (transformer)
            transforms.push(transformer);
    }
    ret["abstractAccessor_" + target + "|" + subclass] = {
        'target': {
            'type': 'CLASS',
            'name': subclass
        },
        'transformer': function (node) {
            for (var x = 0; x < transforms.length; x++) {
                var method = transforms[x]();
                var oldNode = findMethodNode(node, method.name, method.desc);
                // copy over annotations
                method.visibleAnnotations = oldNode.visibleAnnotations;
                method.invisibleAnnotations = oldNode.invisibleAnnotations;
                method.visibleTypeAnnotations = oldNode.visibleTypeAnnotations;
                method.invisibleTypeAnnotations = oldNode.invisibleTypeAnnotations;
                method.visibleParameterAnnotations = oldNode.visibleParameterAnnotations;
                method.invisibleParameterAnnotations = oldNode.invisibleParameterAnnotations;
                node.methods.remove(oldNode);
                node.methods.add(method);
            }
            return node;
        }
    };
}

function createAbstractTransform(ret, target, intermediary, accessor) {
    switch (accessor.type.toLocaleLowerCase()) {
        case 'field':
            var fieldOwner = !accessor.field.actualOwner ? target : accessor.field.actualOwner;
            ret["abstractAccessIncreaseField_" + fieldOwner + "#" + accessor.field.name] = {
                'target': {
                    'type': 'FIELD',
                    'class': fieldOwner,
                    'fieldName': accessor.field.name
                },
                'transformer': function (node) {
                    var access = getAccess(node.access);
                    if (access !== Opcodes.ACC_PROTECTED && access !== Opcodes.ACC_PUBLIC) {
                        node.access = modifyAccess(node.access, Opcodes.ACC_PROTECTED);
                    }
                    return node;
                }
            }
            return createFieldAccessor(ret, target, intermediary, accessor);
        case 'method':
            var methodOwner = !accessor.method.actualOwner ? target : accessor.method.actualOwner;
            ret["abstractAccessIncreaseMethod_" + methodOwner + "#" + accessor.method.name + accessor.method.desc] = {
                'target': {
                    'type': 'METHOD',
                    'class': methodOwner,
                    'fieldName': accessor.field.name
                },
                'transformer': function (node) {
                    var access = getAccess(node.access);
                    if (access !== Opcodes.ACC_PROTECTED && access !== Opcodes.ACC_PUBLIC) {
                        node.access = modifyAccess(node.access, Opcodes.ACC_PROTECTED);
                    }
                    return node;
                }
            }
            return createMethodAccessor(ret, target, intermediary, accessor);
    }
}

function createMethodAccessor(ret, target, intermediary, accessor) {
    var methodOwner = !accessor.method.actualOwner ? target : accessor.method.actualOwner;
    return function () {
        var isStatic = accessor.static;
        var desc = accessor.method.desc;
        var node = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, accessor.name, desc, null, null);
        if (accessor.static)
            node.access = node.access | Opcodes.ACC_STATIC;
        insertDelegateCall(node, isStatic ? intermediary : target, accessor.method.params, isStatic, false, accessor.method.returnType, {
            'clazz': methodOwner,
            'name': ASMAPI.mapMethod(accessor.method.name),
            'desc': accessor.method.desc
        });
        return node;
    }
}

function createFieldAccessor(ret, target, intermediary, accessor) {
    var fieldOwner = !accessor.field.actualOwner ? target : accessor.field.actualOwner;
    var getter = accessor.method.toLocaleLowerCase() == 'get';
    if (!getter) {
        ret["fieldDefinalize_" + target + "#" + accessor.field.name] = {
            'target': {
                'type': 'FIELD',
                'class': fieldOwner,
                'fieldName': accessor.field.name
            },
            'transformer': function (node) {
                node.access = removeModifier(node.access, Opcodes.ACC_FINAL);
                return node;
            }
        }
    }
    return function () {
        var desc = getter ? "()" + accessor.field.desc : "(" + accessor.field.desc + ")V";
        var node = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, accessor.name, desc, null, null);
        var targetType = getType(accessor.field.desc);
        if (accessor.static)
            node.access = node.access | Opcodes.ACC_STATIC;
        if (getter) {
            if (!accessor.static) {
                node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            var opcode = accessor.static ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
            node.instructions.add(new FieldInsnNode(opcode, getInternalName(fieldOwner), ASMAPI.mapField(accessor.field.name), accessor.field.desc));
            node.instructions.add(new InsnNode(targetType.getOpcode(Opcodes.IRETURN)));
            node.visitMaxs(targetType.getSize(), targetType.getSize());
        } else {
            var stackSpace = accessor.static ? 0 : 1; // Stack space for "this"
            var max = stackSpace + targetType.getSize();
            if (!accessor.static) {
                node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            node.instructions.add(new VarInsnNode(targetType.getOpcode(Opcodes.ILOAD), stackSpace));
            var opcode = accessor.static ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD;
            node.instructions.add(new FieldInsnNode(opcode, getInternalName(fieldOwner), ASMAPI.mapField(accessor.field.name), accessor.field.desc));
            node.instructions.add(new InsnNode(Opcodes.RETURN));
            node.visitMaxs(max, max);
        }
        return node;
    }
}
