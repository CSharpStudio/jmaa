function showModel(model) {
    let meta = data[model], fields = [], uniques = [];
    for (let f of meta.fields) {
        if (f.name === 'id') continue;
        fields.push(`<tr>
                        <td>${f.name}</td>
                        <td>${f.label}</td>
                        <td>${f.type}</td>
                        <td>${f.required ? '是' : '否'}</td>
                        <td>${f.store ? '是' : '否'}</td>
                        <td>${f.related || ''}</td>
                        <td>${f.help || ''}</td>
                    </tr>`);
    }
    for (let f of meta.uniques) {
        if (f.name === 'id') continue;
        uniques.push(`<tr>
                        <td>${f.name}</td>
                        <td>${f.fields}</td>
                        <td>${f.message || ''}</td>
                    </tr>`);
    }
    $('.main').html(`
<div class='title'>${model} ${meta.label || ''}</div>
<div class='desc'>${meta.description || ''}</div>
<div class='model'>
    <div class="title">
        <span>字段</span>
    </div>
    <div style="padding: 5px;">
        <table class="model-table" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th>名称</th>
                    <th>标题</th>
                    <th>类型</th>
                    <th>必须</th>
                    <th>存储</th>
                    <th>关联</th>
                    <th>说明</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>id</td>
                    <td>主键</td>
                    <td>char</td>
                    <td>是</td>
                    <td>是</td>
                    <td></td>
                    <td>唯一键,13位字符</td>
                </tr>
                ${fields.join('')}
            </tbody>
        </table>
    </div>
</div>
<div class='model'>
    <div class="title">
        <span>唯一索引</span>
    </div>
    <div style="padding: 5px;">
        <table class="model-table" cellspacing="0" cellpadding="0">
            <thead>
                <tr>
                    <th>名称</th>
                    <th>字段</th>
                    <th>说明</th>
                </tr>
            </thead>
            <tbody>
                ${uniques.join('')}
            </tbody>
        </table>
    </div>
</div>`);
}

function showService(model, service) {
    let meta = data[model], svc = meta.services[service];
    let argsExample = '', argsDoc = '', resultExample = svc.result?.example;
    if (typeof (resultExample) === 'undefined') {
        resultExample = null;
    }
    for (let name in svc.args) {
        if (argsExample) {
            argsExample += `,
        `;
        }
        let arg = svc.args[name];
        argsExample += `"${name}" : ` + JSON.stringify(arg.example, null, true);
        argsDoc += `<tr><td>${name}</td><td>${arg.type}</td><td>${arg.description}</td></tr>`;
    }
    $('.main').html(`
<div class='title'>${model} ${meta.label || ''}</div>
<div class='desc'>${meta.description || ''}</div>
<div class='service'>
    <div class='title'>
        <div class="name">${svc.name}</div>
        <div class="label">${svc.label}</div>
    </div>
    <div class="section">
        <h4>说明：</h4>${svc.desc || ''} (权限码：${svc.auth})
    </div>
    <div class="section">
        <h4>参数：</h4>
        <div style="flex:1">
            <table class="args-table">
                <thead>
                    <tr>
                        <th>名称</th>
                        <th>类型</th>
                        <th>说明</th>
                    </tr>
                </thead>
                <tbody>
                    ${argsDoc}
                </tbody>
            </table>
        </div>
    </div>
    <div class="section">
        <h4>结果：</h4>
        <div style="flex:1">
            <table class="args-table" cellspacing="0" cellpadding="0">
                <thead>
                    <tr>
                        <th>类型</th>
                        <th>说明</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>${svc.result?.type}</td>
                        <td>${svc.result?.description}</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="section">
        <h4 style="flex: 1;">请求：</h4>
        <button class="btn btn-debug" data-key="${model}-${service}">试一试</button>
    </div>
    <div class="debug-code" style="display:none">
        <textarea class="req-box" rows="18" spellcheck="false" style="resize: none; width: 100%;">{
  "id" : "guid",
  "jsonrpc" : "2.0",
  "method" : "${svc.name}",
  "params" : {
    "args" : {
        ${argsExample}
    },
    "context" : {
      "lang" : "zh_CN"
    },
    "model" : "${model}"
  }
}</textarea>
        <button class="btn btn-send" data-key="${model}-${service}">发送</button>
    </div>
    <div class="highlight-code">
        <pre class="microlight">${highlightJSON(`{
  "id" : "guid",
  "jsonrpc" : "2.0",
  "method" : "${svc.name}",
  "params" : {
    "args" : {
        ${argsExample}
    },
    "context" : {
      "lang" : "zh_CN",
      "token" : "&lt;token&gt; 如果有cookie，不需要传token"
    },
    "model" : "${model}"
  }
}`)}</pre>
    </div>
    <div class="section">
        <h4>响应：</h4>
    </div>
    <div class="debug-code" style="display: none;">
        <textarea class="res-box" rows="18" spellcheck="false" style="resize: none; width: 100%;"></textarea>
    </div>
    <div class="highlight-code">
        <pre class="microlight">${highlightJSON(`{
  "id" : "guid",
  "jsonrpc" : "2.0",
  "result" : ${JSON.stringify(resultExample, null, true)}
}`)}</pre>
    </div>

</div>
`);
}

function highlightJSON(json) {
    // 匹配key
    let keyReg = new RegExp("\"(.*)\"(?= :)", "g")
    // 匹配value
    let valueReg = new RegExp("(?<=:\\s.?)(\"(.*)\"|\\d+)", "g")
    if (typeof json === "object" && json !== null) {
        json = JSON.stringify(json, this, 5)
    }
    // 颜色替换
    let res = json.replace(keyReg, (match) => {
        return `<span class="key">${match}</span>`
    }).replace(valueReg, (match) => {
        if (/^[\d\.]+.?$/.test(match)) {
            return `<span class="number">${match}</span>`
        }
        return `<span class="text">${match}</span>`
    })
    return res
}

function cookie(name, value, options) {
    if (typeof value !== 'undefined') {
        options = options || {};
        if (value === null) {
            value = '';
            options = $.extend({}, options);
            options.expires = -1;
        }
        let expires = '';
        if (options.expires && (typeof options.expires === 'number' || options.expires.toUTCString)) {
            let date;
            if (typeof options.expires === 'number') {
                date = new Date();
                date.setTime(date.getTime() + options.expires * 24 * 60 * 60 * 1000);
            } else {
                date = options.expires;
            }
            expires = '; expires=' + date.toUTCString();
        }
        const path = options.path ? '; path=' + options.path : ';path=/';
        const domain = options.domain ? '; domain=' + options.domain : '';
        const secure = options.secure ? '; secure' : '';
        document.cookie = [name, '=', encodeURIComponent(value), expires, path, domain, secure].join('');
    } else {
        let cookieValue = null;
        if (document.cookie && document.cookie != '') {
            const cookies = document.cookie.split(';');
            for (let i = 0; i < cookies.length; i++) {
                const cookie = jQuery.trim(cookies[i]);
                if (cookie.substring(0, name.length + 1) == name + '=') {
                    cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                    break;
                }
            }
        }
        return cookieValue;
    }
}

$(function () {
    $('.aside').on('click', '.menu-title', function () {
        $('.menu-title').removeClass('select');
        $(this).addClass('select').toggleClass('open').next('.menu-subs').toggle();
        var model = $(this).attr('data-model');
        var svc = $(this).attr('data-service');
        if (svc) {
            showService(model, svc);
        } else if (model) {
            showModel(model);
        }
    });
    $('.main').on('click', '.btn-debug', function () {
        let btn = $(this).toggleClass('cancel').text($(this).hasClass('cancel') ? "取消" : "试一试");
        $('.debug-code').toggle();
        $('.highlight-code').toggle();
        let req = localStorage.getItem('api:' + btn.attr("data-key"));
        if (req) {
            $('.req-box').val(req);
        }
    });
    $('.main').on('click', '.btn-send', function () {
        let btn = $(this);
        $('.res-box').val('\u0020\u52a0\u8f7d\u4e2d...');
        let req = $('.req-box').val();
        let url = '/' + tenant + '/rpc/service';
        $.ajax({
            type: "POST",
            dataType: "json",
            data: req,
            contentType: "application/json",
            url: url,
            error: function (XMLHttpRequest, textStatus, errorThrown) {
                $('.res-box').val(XMLHttpRequest.status + ':' + XMLHttpRequest.responseText);
            },
            success: function (data) {
                localStorage.setItem('api:' + btn.attr("data-key"), req);
                $('.res-box').val(JSON.stringify(data, null, 4));
            }
        });
    });
    let token = cookie('ctx_token');
    if (token) {
        let user = JSON.parse(localStorage.getItem("user_info") || '{}');
        $('.login').prepend(`<span>${user.name}</span>`)
        $('.login a').html('退出').on('click', function () {
            $.ajax({
                type: "POST",
                dataType: "json",
                url: '/' + tenant + '/rpc/service',
                data: {
                    id: 'logout',
                    jsonrpc: '2.0',
                    method: 'logout',
                    params: {
                        args: {},
                        model: 'rbac.user'
                    },
                },
                success(r) {
                    cookie('ctx_token', '', {expires: -1});
                    window.location.href = '/' + tenant + '/login';
                },
            });
            return false;
        });
    }
});
