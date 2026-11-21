const time = () => new Date().toTimeString().slice(0, 8) + '.' + String(Date.now() % 1000).padStart(3, '0');

export const log = {
  info: (msg) => console.log(`${time()} INFO  ${msg}`),
  warn: (msg) => console.warn(`${time()} WARN  ${msg}`),
  error: (msg) => console.error(`${time()} ERROR ${msg}`),
};
