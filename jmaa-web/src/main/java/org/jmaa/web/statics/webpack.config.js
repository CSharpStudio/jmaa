const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerWebpackPlugin = require("css-minimizer-webpack-plugin");
const entries = [
    {name: 'jmaa', files: './jmaa.js', path: './'},
    {name: 'login', files: ['./login/login.js', './login/css/login.css'], path: './login/'},
    {name: 'app', files: ['./web/app.js', './web/css/app.css'], path: './web/'},
    {name: 'view', files: ['./web/view.js', './web/css/view.css'], path: './web/'},
    {name: 'app', files: ['./mobile/app.js', './mobile/css/app.css'], path: './mobile/'},
    {name: 'view', files: ['./mobile/view.js', './mobile/css/view.css'], path: './mobile/'},
    {name: 'app', files: ['./pad/app.js', './pad/css/app.css'], path: './pad/'},
    {name: 'diagram', files: ['./diagram/diagram.js', './diagram/diagram.css'], path: './diagram/'},
];
const generateConfig = () => {
    const configs = [];
    for (const e of entries) {
        let entry = {};
        entry[e.name] = e.files;
        configs.push({
            mode: 'production',
            target: ['web', 'es5'], // 必须
            entry,
            output: {
                path: path.resolve(__dirname, 'assets', e.path),
                filename: `${e.name}.min.js`
            },
            module: {
                rules: [
                    {
                        test: /\.js$/,
                        exclude: /node_modules/,
                        use: {
                            loader: 'babel-loader',
                            options: {
                                presets: [
                                    [
                                        '@babel/preset-env',
                                        {
                                            useBuiltIns: 'usage',
                                            corejs: {
                                                version: 3,
                                            },
                                            //targets: "> 0.25%, not dead",
                                            targets: {
                                                ie: 11,
                                                chrome: 50,
                                                firefox: 50,
                                            },
                                        },
                                    ],
                                ],
                            },
                        },
                    }, {
                        test: /\.css$/,
                        use: [{
                            loader: MiniCssExtractPlugin.loader,
                            options: {
                                publicPath: '/web/org/jmaa/base/statics/'
                            }
                        },
                            {
                                loader: 'css-loader',
                                options: {
                                    // 使用一个函数来处理url
                                    url: false
                                }
                            },
                        ]
                    }, {
                        test: /\.(png|svg|jepg|gif)/,
                        type: 'asset/resource'
                    }
                ],
            },
            plugins: [
                new MiniCssExtractPlugin(),
                new CssMinimizerWebpackPlugin()
            ]
        });
    }
    return configs;
};

module.exports = generateConfig();

// npx webpack --config webpack.config.js
