// Част 1. "Синхронен" клиент: всяка заявка се изчаква с await, преди да тръгне следващата.
import { ClientError, ServerUnavailableError } from './errors.js';
import { log } from './log.js';
import { UserClient } from './userClient.js';

const client = new UserClient(process.argv[2] ?? 'http://localhost:8000');

try {
  const me = await client.create({ name: 'Иван Иванов', age: 17, email: 'ivan@elsys.bg', status: 'pending' });
  log.info(`Създаден: ${JSON.stringify(me)}`);

  const active = await client.patch(me.id, { status: 'active' });
  log.info(`След PATCH: ${JSON.stringify(active)}`);

  log.info(`Изтрит: ${await client.delete(me.id)}`);
  log.info(`getById след изтриване: ${await client.getById(me.id)}`);
  log.info(`Повторно изтриване: ${await client.delete(me.id)}`);

  // Въпрос 1.2: невалидно поле -> 400 -> ClientError
  const other = await client.create({ name: 'Петър Петров', age: 16, email: 'petar@elsys.bg', status: 'pending' });
  try {
    await client.patch(other.id, { age: 'abc' });
  } catch (error) {
    if (!(error instanceof ClientError)) throw error;
    log.warn(`PATCH age="abc" -> ClientError, status ${error.status}, body ${error.body}`);
  } finally {
    await client.delete(other.id);
  }

  log.info(`Всички потребители: ${JSON.stringify(await client.getAll())}`);
} catch (error) {
  if (!(error instanceof ServerUnavailableError)) throw error;
  log.error(`Сървърът не е достъпен: ${error.message}`);
}
