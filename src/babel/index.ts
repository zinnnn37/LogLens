// babel/index.ts
import type { PluginObj, NodePath } from '@babel/core';
import { types as t } from '@babel/core';

export default function babelPluginLoglens(): PluginObj {
  return {
    name: 'loglens-auto-instrument',
    visitor: {
      // withLogLens import 자동 주입
      Program(path: NodePath) {
        const hasWithLogLens = (path.node as any).body.some((node: any) => {
          if (node.type === 'ImportDeclaration') {
            return node.specifiers.some(
              (spec: any) => spec.imported?.name === 'withLogLens',
            );
          }
          return false;
        });

        // withLogLens import 없으면 추가
        if (!hasWithLogLens) {
          const importDeclaration = t.importDeclaration(
            [
              t.importSpecifier(
                t.identifier('withLogLens'),
                t.identifier('withLogLens'),
              ),
            ],
            t.stringLiteral('soo1-loglens'),
          );
          (path.node as any).body.unshift(importDeclaration);
        }
      },

      FunctionDeclaration(path: NodePath) {
        if (shouldInstrument(path)) {
          const functionName = (path.node as any).id.name;
          wrapWithLogLens(path, functionName, t);
        }
      },

      VariableDeclarator(path: NodePath) {
        const node = path.node as any;

        if (!t.isIdentifier(node.id)) return;

        const init = node.init;
        if (!init) return;

        // withLogLens 호출은 바로 처리 (shouldInstrumentExpression 체크 안 함)
        if (
          t.isCallExpression(init) &&
          t.isIdentifier(init.callee) &&
          init.callee.name === 'withLogLens'
        ) {
          const functionName = node.id.name;
          wrapVariableFunction(path, functionName, t); // ← 바로 호출
          return;
        }

        // ArrowFunctionExpression 또는 FunctionExpression
        if (t.isArrowFunctionExpression(init) || t.isFunctionExpression(init)) {
          if (shouldInstrumentExpression(path)) {
            const functionName = node.id.name;
            wrapVariableFunction(path, functionName, t);
          }
        }
      },
    },
  };
}

function shouldInstrument(path: any): boolean {
  // 이미 withLogLens로 래핑된 함수는 스킵
  const parent = path.findParent(
    (p: any) => p.isCallExpression() && p.node.callee.name === 'withLogLens',
  );
  if (parent) return false;

  // getter/setter/constructor 제외
  if (
    path.node.kind === 'get' ||
    path.node.kind === 'set' ||
    path.node.kind === 'constructor'
  ) {
    return false;
  }

  // node_modules 제외
  const filename = path.hub.file.opts.filename;
  if (filename && filename.includes('node_modules')) {
    return false;
  }

  return true;
}

function shouldInstrumentExpression(path: any): boolean {
  const node = path.node as any;
  const init = node.init;

  // init이 이미 withLogLens 호출이면 스킵
  if (
    t.isCallExpression(init) &&
    t.isIdentifier(init.callee) &&
    init.callee.name === 'withLogLens'
  ) {
    return false;
  }

  // node_modules 제외
  const filename = path.hub.file.opts.filename;
  if (filename && filename.includes('node_modules')) {
    return false;
  }

  return true;
}

// 함수 선언을 withLogLens로 래핑
function wrapWithLogLens(path: any, functionName: string, t: any): void {
  const functionNode = path.node;

  // 원본 함수를 FunctionExpression으로 변환
  const wrappedFunction = t.functionExpression(
    null, // id (익명)
    functionNode.params,
    functionNode.body,
    functionNode.generator,
    functionNode.async,
  );

  // withLogLens(function() { ... }, { logger: 'functionName' })
  const callExpression = t.callExpression(t.identifier('withLogLens'), [
    wrappedFunction,
    t.objectExpression([
      t.objectProperty(t.identifier('logger'), t.stringLiteral(functionName)),
    ]),
  ]);

  // 원본 함수 선언을 변수 선언으로 대체
  path.replaceWith(
    t.variableDeclaration('const', [
      t.variableDeclarator(t.identifier(functionName), callExpression),
    ]),
  );
}

function wrapVariableFunction(path: any, functionName: string, t: any): void {
  const node = path.node as any;
  const functionNode = node.init;

  // ✅ 이미 withLogLens로 래핑됐는지 확인
  if (
    t.isCallExpression(functionNode) &&
    t.isIdentifier(functionNode.callee) &&
    functionNode.callee.name === 'withLogLens'
  ) {
    // 이미 래핑됐지만 logger 옵션 없으면 추가
    const args = functionNode.arguments;

    if (args.length === 1) {
      // logger 옵션 없음 → 추가
      args.push(
        t.objectExpression([
          t.objectProperty(
            t.identifier('logger'),
            t.stringLiteral(functionName),
          ),
        ]),
      );
    } else if (args.length === 2 && t.isObjectExpression(args[1])) {
      // logger 옵션 있는지 확인
      const options = args[1];
      const hasLogger = options.properties.some(
        (prop: any) => t.isIdentifier(prop.key) && prop.key.name === 'logger',
      );

      if (!hasLogger) {
        // logger 속성 추가
        options.properties.push(
          t.objectProperty(
            t.identifier('logger'),
            t.stringLiteral(functionName),
          ),
        );
      }
    }
    return;
  }

  // 래핑 안 됐으면 기존 로직
  const callExpression = t.callExpression(t.identifier('withLogLens'), [
    functionNode,
    t.objectExpression([
      t.objectProperty(t.identifier('logger'), t.stringLiteral(functionName)),
    ]),
  ]);

  node.init = callExpression;
}
