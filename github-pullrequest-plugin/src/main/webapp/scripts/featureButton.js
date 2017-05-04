function callFeature(button, answerPlaceId, parameters) {
    new Ajax.Request(button.action, {
        method: "post",
        parameters: parameters,
        onComplete: function (rsp) {
            answerPlaceId.innerHTML = rsp.responseText;
        }
    });
    return false;
}

