const http = require('http');

const loginData = JSON.stringify({ username: 'corner', password: 'corner123' });

const req = http.request({
  hostname: 'localhost',
  port: 8080,
  path: '/api/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': loginData.length
  }
}, (res) => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    let token;
    try {
      token = JSON.parse(data).token;
    } catch(e) { token = data; }

    http.get('http://localhost:8080/api/cucina/orders/active', {
      headers: { 'Authorization': 'Bearer ' + token }
    }, (res2) => {
      let data2 = '';
      res2.on('data', chunk => data2 += chunk);
      res2.on('end', () => {
        console.log("ACTIVE ORDERS:", data2);
      });
    });
  });
});

req.write(loginData);
req.end();
