// babel/index.ts
import type { PluginObj, NodePath } from '@babel/core';
import { types as t } from '@babel/core';

export default function babelPluginLoglens(): PluginObj {
  return {
    name: 'loglens-auto-instrument',
    visitor: {
      FunctionDeclaration(path: NodePath) {
        if (shouldInstrument(path)) {
          const functionName = (path.node as any).id.name;
          instrumentFunction(path, functionName, t);
        }
      },

      ArrowFunctionExpression(path: NodePath) {
        if (shouldInstrument(path)) {
          const name = getFunctionName(path, t);
          if (name !== 'anonymous') {
            instrumentFunction(path, name, t);
          }
        }
      },

      FunctionExpression(path: NodePath) {
        if (shouldInstrument(path)) {
          const name = getFunctionName(path, t);
          if (name !== 'anonymous') {
            instrumentFunction(path, name, t);
          }
        }
      },
    },
  };
}

function shouldInstrument(path: any): boolean {
  const parent = path.findParent(
    (p: any) => p.isCallExpression() && p.node.callee.name === 'withLogLens',
  );
  if (parent) return false;

  if (
    path.node.kind === 'get' ||
    path.node.kind === 'set' ||
    path.node.kind === 'constructor'
  ) {
    return false;
  }

  const filename = path.hub.file.opts.filename;
  if (filename && filename.includes('node_modules')) {
    return false;
  }

  return true;
}

function getFunctionName(path: any, t: any): string {
  const variableDeclarator = path.findParent((p: any) =>
    p.isVariableDeclarator(),
  );
  if (variableDeclarator && t.isIdentifier(variableDeclarator.node.id)) {
    return variableDeclarator.node.id.name;
  }

  const objectProperty = path.findParent((p: any) => p.isObjectProperty());
  if (objectProperty && t.isIdentifier(objectProperty.node.key)) {
    return objectProperty.node.key.name;
  }

  const classMethod = path.findParent((p: any) => p.isClassMethod());
  if (classMethod && t.isIdentifier(classMethod.node.key)) {
    return classMethod.node.key.name;
  }

  return 'anonymous';
}

function instrumentFunction(path: any, functionName: string, t: any): void {
  const body = path.node.body;

  if (!t.isBlockStatement(body)) {
    const returnValue = body;
    path.node.body = t.blockStatement([
      createStartLog(functionName, t),
      createEndLog(functionName, t),
      t.returnStatement(returnValue),
    ]);
    return;
  }

  const blockBody = body.body;

  if (blockBody.length === 0) return;

  blockBody.unshift(createStartLog(functionName, t));

  path.traverse({
    ReturnStatement(returnPath: any) {
      if (returnPath.getFunctionParent() !== path) {
        return;
      }
      returnPath.insertBefore(createEndLog(functionName, t));
    },
  });

  const lastStatement = blockBody[blockBody.length - 1];
  if (!t.isReturnStatement(lastStatement)) {
    blockBody.push(createEndLog(functionName, t));
  }
}

function createStartLog(functionName: string, t: any): any {
  return t.expressionStatement(
    t.callExpression(
      t.memberExpression(t.identifier('loglens'), t.identifier('info')),
      [t.stringLiteral(`${functionName} called`)],
    ),
  );
}

function createEndLog(functionName: string, t: any): any {
  return t.expressionStatement(
    t.callExpression(
      t.memberExpression(t.identifier('loglens'), t.identifier('info')),
      [t.stringLiteral(`${functionName} completed`)],
    ),
  );
}
