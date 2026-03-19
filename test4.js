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

    http.get('http://localhost:8080/api/cameriere/menu/items', {
      headers: { 'Authorization': 'Bearer ' + token }
    }, (res2) => {
      let data2 = '';
      res2.on('data', chunk => data2 += chunk);
      res2.on('end', () => {
        console.log("ITEMS CODE:", res2.statusCode);
        console.log("ITEMS BODY:", data2.substring(0, 100)); // check start of body
        
        http.get('http://localhost:8080/api/cameriere/menu/categories', {
          headers: { 'Authorization': 'Bearer ' + token }
        }, (res3) => {
             let data3 = '';
             res3.on('data', chunk => data3 += chunk);
             res3.on('end', () => {
                 console.log("CATEGORIES CODE:", res3.statusCode);
                 console.log("CATEGORIES BODY:", data3.substring(0, 100));
             });
        });

      });
    });
  });
});

req.write(loginData);
req.end();
