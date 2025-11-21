// src/decorators/log.ts

function log(target: Function, context: ClassMethodDecoratorContext) {
  const methodName = String(context.name);

  return function (this: any, ...args: any[]) {
    console.log(`[LOG] ${methodName}`, args);
    const result = target.apply(this, args);
    console.log(`[RESULT] ${methodName}`, result);
    return result;
  };
}

export { log as Log };
