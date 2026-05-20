import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = {
  stages: [
    { duration: '5s', target: 30 },
    { duration: '15s', target: 30 },
    { duration: '5s', target: 0 }
  ]
};
export default function () {
  const url = 'http://host.docker.internal:8080/shorten';
  const o1 = Math.floor(Math.random() * 254) + 1;
  const o2 = Math.floor(Math.random() * 254) + 1;
  const o3 = Math.floor(Math.random() * 254) + 1;
  const simulatedIp = o1 + '.' + o2 + '.' + o3 + '.1';
  const randomId = Math.floor(Math.random() * 1000000);
  const payload = JSON.stringify( { longUrl: 'www.test-uncapped-' + randomId + '.com' } );
  const params = { headers: { 'Content-Type': 'application/json', 'X-Forwarded-For': simulatedIp } };
  const res = http.post(url, payload, params);
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.01);
}
