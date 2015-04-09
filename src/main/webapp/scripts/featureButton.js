function callFeature(button, answerPlaceId, parameters) {
    event.preventDefault();
    new Ajax.Request(button.action, {
        method: "post",
        parameters : parameters,
        onComplete: function(rsp) {
            answerPlaceId.innerHTML = rsp.responseText;
        }
    });
}

