// Част 3. Retry.
// A) Спрете сървъра, пуснете `npm run retry` и пуснете сървъра до ~15 s -> успех.
// B) Не пускайте сървъра -> грешка след 5 опита.
import { log } from './log.js';
import { withRetry } from './retry.js';
import { newRequestId, UserClient } from './userClient.js';

const MAX_ATTEMPTS = 5;
const client = new UserClient(process.argv[2] ?? 'http://localhost:8000');
const student = { name: 'Иван Иванов', age: 17, email: 'ivan@elsys.bg', status: 'pending' };

// Same X-Request-Id for every attempt: it is one logical request.
const requestId = newRequestId();
log.info(`Записване на ${student.name} с X-Request-Id: ${requestId} (до ${MAX_ATTEMPTS} опита)`);

withRetry(() => client.create(student, requestId), MAX_ATTEMPTS)
  .then((user) => log.info(`Успех: ${JSON.stringify(user)}`))
  .catch((error) => log.error(`Неуспех: ${error.message}`));
