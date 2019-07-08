let http = require('http'); //用来启服务
let fs = require('fs'); //用来读取文件
const root = __dirname;//你本地放index.html页面的文件路径
const port = 3000;

//开启服务
var server = http.createServer(function (req, res) {
    var url = req.url;
    if (url === '/') {
        //请求 index.html 返回请求状态为200
        var data = fs.readFileSync(root + '/index.html').toString();//读取文件内容并转换为字符串
        res.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
        res.end(data);
    } else if (url === '/favicon.ico') {
        var icon = "base64;";
        res.writeHead(200, {'Content-Type': 'image/x-icon'});
        res.end(icon)
    } else {
        var file = root + url;
        fs.readFile(file, function (err, data) {
            if (err) {
                res.writeHeader(404, {
                    'content-type': 'text/html;charset="utf-8"'
                });
                // res.write();
                res.end('<h1>404错误</h1><p>你要找的页面不存在1</p>');
            } else {
                res.writeHeader(200, {
                    'content-type': 'text/html;charset="utf-8"'
                });
                // res.write();
                res.end(data);

            }
        })
    }
}).listen(port); //端口号


//获取本地ip地址
getIPAdress = () => {
    let interfaces = require('os').networkInterfaces();
    let ips = [];
    for (let devName in interfaces) {
        let iface = interfaces[devName];
        for (var i = 0; i < iface.length; i++) {
            let alias = iface[i];
            if (alias.family === 'IPv4' && alias.address !== '127.0.0.1' && !alias.internal) {
                console.log(alias.address);
                ips.push(alias.address)
            }
        }
    }
    return ips;
};

console.log('服务器开启在：http://' + getIPAdress()[0] + ':' + port + '/');