// Част 2. Асинхронен клиент: вериги от Promise-и (аналог на CompletableFuture).
//   thenApply / thenCompose  -> .then(...)   (ако върнем Promise, .then го "разгъва" автоматично)
//   thenCombine / allOf      -> Promise.all([...])
//   exceptionally / handle   -> .catch(...) / .then(onSuccess, onError)
import { ClientError } from './errors.js';
import { log } from './log.js';
import { UserClient } from './userClient.js';

const client = new UserClient(process.argv[2] ?? 'http://localhost:8000');

// 1. thenCompose: всяка следваща заявка зависи от резултата на предишната.
function confirmRegistration() {
  return client.create({ name: 'Мария Петрова', age: 16, email: 'maria@elsys.bg', status: 'pending' })
    .then((user) => client.patch(user.id, { status: 'confirmed' }))
    .then((user) => client.getById(user.id))
    .then((user) => log.info(`1) Потвърдено записване: ${JSON.stringify(user)}`));
}

// 2. thenCombine: двете GET заявки тръгват паралелно и не зависят една от друга.
async function olderOfTwo() {
  const georgi = await client.create({ name: 'Георги Димитров', age: 17, email: 'georgi@elsys.bg', status: 'pending' });
  const elena = await client.create({ name: 'Елена Николова', age: 15, email: 'elena@elsys.bg', status: 'pending' });

  return Promise.all([client.getById(georgi.id), client.getById(elena.id)])
    .then(([a, b]) => (a.age >= b.age ? a : b))
    .then((older) => log.info(`2) По-възрастен е ${older.name} (${older.age} г.)`));
}

// 3. handle: превръща и резултата, и грешката в разбираемо съобщение.
function describe(promise) {
  return promise.then(
    (user) => (user ? `намерен ${user.name}` : 'няма такъв потребител (HTTP 404)'),
    (error) => (error instanceof ClientError
      ? `невалидна заявка (HTTP ${error.status}): ${error.body}`
      : `грешка: ${error.message}`),
  );
}

function friendlyErrors() {
  return Promise.all([describe(client.getById(crypto.randomUUID())), describe(client.getById('abc'))])
    .then(([missing, invalid]) => {
      log.info(`3) Несъществуващ UUID: ${missing}`);
      log.info(`3) Id "abc": ${invalid}`);
    });
}

confirmRegistration()
  .then(olderOfTwo)
  .then(friendlyErrors)
  .catch((error) => log.error(`Неочаквана грешка: ${error.message}`));
