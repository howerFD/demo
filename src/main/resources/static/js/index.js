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
        dataType:"json",
        data: JSON.stringify($("#config-form").serializeJson()),
        contentType:"application/json;charset=utf-8",
        async: false,
        success: function (result) {
            $("#submitBtn").removeAttr("disabled");
            if (result && result.success) {
                window.open(result.payload);
            } else {
                alert(result.msg || "操作失败");
            }
        }
    });
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