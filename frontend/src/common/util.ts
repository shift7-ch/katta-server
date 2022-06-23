/**
 * Create a new random UUID
 * @returns A (version 4) UUID
 */
export function uuid(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

export class Deferred<T> {

  public promise: Promise<T>;
  public reject: (reason?: any) => void;
  public resolve: (value: T) => void;

  constructor() {
    this.reject = (reason) => { };
    this.resolve = (value) => { };
    this.promise = new Promise<T>((resolve, reject) => {
      this.reject = reject;
      this.resolve = resolve;
    });
  }

}

/**
 * Creates a cancellable debounced function that delays invoking the provided function until at least `wait` milliseconds have elapsed since the last time it was invoked.
 * Sources: https://decipher.dev/30-seconds-of-typescript/docs/debounce/ and https://wiki.selfhtml.org/wiki/JavaScript/Tutorials/Debounce_und_Throttle
 * @param func function to debounce
 * @param wait time to wait before calling function
 * @returns debounced function
 */
export const debounce = (func: Function, wait = 300) => {
  let timeoutId: ReturnType<typeof setTimeout>;
  function debounceCore(this: any, ...args: any[]) {
    cancel();
    timeoutId = setTimeout(() => func.apply(this, args), wait);
  }
  function cancel() {
    clearTimeout(timeoutId);
  }
  debounceCore.cancel = cancel;
  return debounceCore;
};
