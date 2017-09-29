$(function () {
    recoverStore();
});

var rowid = 0;

// 增加一列表名
function addItem() {
    rowid++;
    var item = $('#row-tpl').attr('rowid', rowid).html();
    $("#gen-btn").before(item);
}

//删除一列表名
function removeItem(para) {
    rowid--;
    $(para).parents('.tab-items:eq(0)').remove();
}

//生成并下载
function doSubmit() {
    $("#form").validate();
    if ($("#config-form").valid(this, '填写信息不完整。') == false) {
        return;
    }
    if (typeof($("#submitBtn")) != "undefined") {
        $("#submitBtn").attr("disabled", "disabled");
    }

    storeToCookie();

    var tableItems = [], modelNames = [];
    if (rowid > 0) {
        $('.tab-items').each(function (k, v) {
            var tableName = $(v).find('input:eq(0)').val();
            var modelName = $(v).find('input:eq(1)').val();

            tableItems.push(tableName);
            modelNames.push(modelName);
        });
        $("#config-form #tableNamesString").val(tableItems.join(','));
        $("#config-form #modelNamesString").val(modelNames.join(','));
    }

    // 转成对象
    (function($){
        $.fn.serializeJson=function(){
            var serializeObj={};
            $(this.serializeArray()).each(function(){
                serializeObj[this.name]=this.value;
            });
            return serializeObj;
        };
    })(jQuery);

    $.ajax({
        url: '/demo/generate',
        type: 'POST',
        data: JSON.stringify($("#config-form").serializeJson()),
        contentType:"application/json;charset=utf-8",
        async: false,
        success: function (data, status, xhr) {
            $("#submitBtn").removeAttr("disabled");
            console.log(data); // ajax方式请求的数据只能存放在javascipt内存空间，可以通过javascript访问，但是无法保存到硬盘
            console.log(status);
            console.log(xhr);
            console.log("=====================");

            downloadFile(data, xhr.getResponseHeader);
        }
    });
}

var getMineType = function(headers, defMineType) {
    return headers('content-type') || defMineType;
};

var getFileName = function(headers, defFileName) {
    var contentDisposition = decodeURI(headers('Content-Disposition'));
    // Firefox, Opera, Chrome
    var fileName = contentDisposition.replace(new RegExp('(\\S+UTF-8\\\'\\\')',"gm"),'');
    // IE
    fileName = fileName.replace(new RegExp('(\\S+filename\\=\\")',"gm"),'');
    if(fileName.lastIndexOf('"') == fileName.length - 1) {
        fileName = fileName.subString(0, fileName.length - 1);
    }

    return fileName || defFileName;
};


var downloadFile =  function (data, headers, mimeType, fileName) {
    var success = false;
    mimeType = getMineType(headers, mimeType);
    fileName = getFileName(headers, fileName);
    var blob = new Blob([data], { type: mimeType });
    try {
        if (navigator.msSaveBlob)
            navigator.msSaveBlob(blob, fileName);
        else {
            // Try using other saveBlob implementations, if available
            var saveBlob = navigator.webkitSaveBlob || navigator.mozSaveBlob || navigator.saveBlob;
            if (saveBlob === undefined) throw "Not supported";
            saveBlob(blob, fileName);
        }
        success = true;
    } catch (ex) {
        console.log("saveBlob method failed with the following exception:");
        console.log(ex);
    }

    if (!success) {
        // Get the blob url creator
        var urlCreator = window.URL || window.webkitURL || window.mozURL || window.msURL;
        if (urlCreator) {
            // Try to use a download link
            var link = document.createElement('a');
            if ('download' in link) {
                // Try to simulate a click
                try {
                    // Prepare a blob URL
                    var url = urlCreator.createObjectURL(blob);
                    link.setAttribute('href', url);

                    // Set the download attribute (Supported in Chrome 14+ / Firefox 20+)
                    link.setAttribute("download", fileName);

                    // Simulate clicking the download link
                    var event = document.createEvent('MouseEvents');
                    event.initMouseEvent('click', true, true, window, 1, 0, 0, 0, 0, false, false, false, false, 0, null);
                    link.dispatchEvent(event);
                    success = true;

                } catch (ex) {
                    console.log("Download link method with simulated click failed with the following exception:");
                    console.log(ex);
                }
            }

            if (!success) {
                // Fallback to window.location method
                try {
                    // Prepare a blob URL
                    // Use application/octet-stream when using window.location to force download
                    var url = urlCreator.createObjectURL(blob);
                    window.location = url;
                    console.log("Download link method with window.location succeeded");
                    success = true;
                } catch (ex) {
                    console.log("Download link method with window.location failed with the following exception:");
                    console.log(ex);
                }
            }
        }
    }

    if (!success) {
        // Fallback to window.open method
        console.log("No methods worked for saving the arraybuffer, using last resort window.open");
        window.open("", '_blank', '');
    }
}

function changeCkbox(obj, id) {
    var checked = $(obj).is(":checked");
    if (checked) {
        $(id).val('1');
        if (id == 'isAlltable') {
            $('#add-item').hide();
            $('.tab-items').remove();
        }
    } else {
        $(id).val('0');
        if (id == 'isAlltable') {
            $('#add-item').show();
            addItem();
        }
    }
}

function recoverStore() {
    $("#config-form input").each(function (k, v) {
        $(v).val($.cookie("MC_" + $(v).attr('name')));
    });
}

function storeToCookie() {
    values = $("#config-form").serializeArray();
    var values, index;
    for (index = 0; index < values.length; ++index) {
        $.cookie("MC_" + values[index].name, values[index].value, {expires: 7});
    }
}