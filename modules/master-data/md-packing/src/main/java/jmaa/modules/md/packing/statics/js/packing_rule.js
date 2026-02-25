//@ sourceURL=packing_rule.js
jmaa.view({
    onCreateLevel(e, form) {
        let seq = 1;
        if (form.owner.data) {
            for (let row of form.owner.data) {
                if (Number(row.seq) >= seq) {
                    seq = Number(row.seq) + 1;
                }
            }
        }
        return {seq}
    },
});
