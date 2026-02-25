jmaa.widgets = {};
jmaa.widget = function (name, define) {
    jmaa.widgets[name] = jmaa.component("widgets." + name, define);
};
