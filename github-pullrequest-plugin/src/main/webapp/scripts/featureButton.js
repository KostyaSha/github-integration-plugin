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
    return false;
}

