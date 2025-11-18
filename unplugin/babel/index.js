"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = babelPluginLoglens;
var core_1 = require("@babel/core");
function babelPluginLoglens() {
    return {
        name: 'loglens-auto-instrument',
        visitor: {
            FunctionDeclaration: function (path) {
                if (shouldInstrument(path)) {
                    var functionName = path.node.id.name;
                    instrumentFunction(path, functionName, core_1.types);
                }
            },
            ArrowFunctionExpression: function (path) {
                if (shouldInstrument(path)) {
                    var name_1 = getFunctionName(path, core_1.types);
                    if (name_1 !== 'anonymous') {
                        instrumentFunction(path, name_1, core_1.types);
                    }
                }
            },
            FunctionExpression: function (path) {
                if (shouldInstrument(path)) {
                    var name_2 = getFunctionName(path, core_1.types);
                    if (name_2 !== 'anonymous') {
                        instrumentFunction(path, name_2, core_1.types);
                    }
                }
            },
        },
    };
}
function shouldInstrument(path) {
    var parent = path.findParent(function (p) { return p.isCallExpression() && p.node.callee.name === 'withLogLens'; });
    if (parent)
        return false;
    if (path.node.kind === 'get' ||
        path.node.kind === 'set' ||
        path.node.kind === 'constructor') {
        return false;
    }
    var filename = path.hub.file.opts.filename;
    if (filename && filename.includes('node_modules')) {
        return false;
    }
    return true;
}
function getFunctionName(path, t) {
    var variableDeclarator = path.findParent(function (p) {
        return p.isVariableDeclarator();
    });
    if (variableDeclarator && t.isIdentifier(variableDeclarator.node.id)) {
        return variableDeclarator.node.id.name;
    }
    var objectProperty = path.findParent(function (p) { return p.isObjectProperty(); });
    if (objectProperty && t.isIdentifier(objectProperty.node.key)) {
        return objectProperty.node.key.name;
    }
    var classMethod = path.findParent(function (p) { return p.isClassMethod(); });
    if (classMethod && t.isIdentifier(classMethod.node.key)) {
        return classMethod.node.key.name;
    }
    return 'anonymous';
}
function instrumentFunction(path, functionName, t) {
    var body = path.node.body;
    if (!t.isBlockStatement(body)) {
        var returnValue = body;
        path.node.body = t.blockStatement([
            createStartLog(functionName, t),
            createEndLog(functionName, t),
            t.returnStatement(returnValue),
        ]);
        return;
    }
    var blockBody = body.body;
    if (blockBody.length === 0)
        return;
    blockBody.unshift(createStartLog(functionName, t));
    path.traverse({
        ReturnStatement: function (returnPath) {
            if (returnPath.getFunctionParent() !== path) {
                return;
            }
            returnPath.insertBefore(createEndLog(functionName, t));
        },
    });
    var lastStatement = blockBody[blockBody.length - 1];
    if (!t.isReturnStatement(lastStatement)) {
        blockBody.push(createEndLog(functionName, t));
    }
}
function createStartLog(functionName, t) {
    return t.expressionStatement(t.callExpression(t.memberExpression(t.identifier('loglens'), t.identifier('info')), [t.stringLiteral("".concat(functionName, " called"))]));
}
function createEndLog(functionName, t) {
    return t.expressionStatement(t.callExpression(t.memberExpression(t.identifier('loglens'), t.identifier('info')), [t.stringLiteral("".concat(functionName, " completed"))]));
}
