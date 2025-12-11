function callFeature(button, answerPlaceId, parameters) {
    fetch(button.action, {
        method: "post",
        headers: crumb.wrap({
          "Content-Type": "application/x-www-form-urlencoded",
        }),
        body: new URLSearchParams(parameters),
    }).then(rsp => {
        rsp.text().then((responseText) => {
            answerPlaceId.innerHTML = responseText;
        });
    });
}

document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('form.callFeature').forEach(function (form) {
        form.onsubmit = (evt) => {
            evt.preventDefault();
            let parameters = JSON.parse(form.dataset.parameters);
            let answerPlaceId = form.dataset.answerPlaceId;
            let self = form;

            callFeature(self, answerPlaceId, parameters);
        };
    });
});
