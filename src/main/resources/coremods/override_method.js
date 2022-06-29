var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');
var Label = Java.type('org.objectweb.asm.Label');

var acc = [];

function initializeCoreMod() {
    ASMAPI.loadFile('coremods/utils.js');

    var data = ASMAPI.loadData('coremods/override_method.json');
    var ret = {};
    for (var name in data) {
        var dt = data[name];
        var input = dt.input;
        var target = dt.target;
        var method = dt.method;
        makeProtected(ret, target.clazz, target.method, method.desc);
        override(ret, method, target, input);
    }
    return ret;
}

function override(ret, method, target, input) {
    ret["override_" + target.clazz + "#" + target.method + method.desc + "|" + input.clazz + "#" + input.method + method.desc] = {
        'target': {
            'type': 'CLASS',
            'name': input.clazz
        },
        'transformer': function (node) {
            var methodVisitor = new MethodNode(acc[target.clazz + "#" + target.method + method.desc], ASMAPI.mapMethod(target.method), method.desc, null, null);
            insertDelegateCall(
                methodVisitor, input.clazz, method.params,
                false, input.interface, method.returnType,
                {
                    'name': input.method,
                    'desc': method.desc
                }
            )
            node.methods.add(methodVisitor);
            return node;
        }
    }
}

function makeProtected(ret, className, method, desc) {
    var fullName = className + "#" + method + desc;
    ret["protected_" + fullName] = {
        'target': {
            'type': 'METHOD',
            'class': className,
            'methodName': method,
            'methodDesc': desc
        },
        'transformer': function (node) {
            node.access = removeModifier(node.access, Opcodes.ACC_FINAL)
            var pb = hasModifier(node.access, Opcodes.ACC_PUBLIC);
            if (pb) {
                acc[fullName] = Opcodes.ACC_PUBLIC;
                return node;
            }
            if (!hasModifier(node.access, Opcodes.ACC_PROTECTED)) {
                node.access = modifyAccess(node.access, Opcodes.ACC_PROTECTED);
            }
            acc[fullName] = getAccess(node.access);
            return node;
        }
    }
}