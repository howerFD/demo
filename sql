/*****************************************************************************/
/*                                                                           */
/*      プロシージャ   ： fix_error_by_dia                                   */
/*                                                                           */
/*      処理概要       ：エラーデータの消去                                  */
/*                                                                           */
/*****************************************************************************/
CREATE OR REPLACE LANGUAGE plpgsql;
CREATE OR REPLACE FUNCTION fix_error_by_dia(diaID IN integer) RETURNS integer 
AS
$BODY$
DECLARE
 train d_train%ROWTYPE;
BEGIN

FOR train IN

SELECT * FROM d_train t WHERE t.suji_div = 1 and t.dia_id = diaID
                              and t.train_no NOT IN (SELECT d.train_no FROM d_operation d where t.aria_cd = d.aria_cd and t.dia_id = d.dia_id)
                              and t.train_no NOT IN (SELECT d.train_no FROM d_operation_ope d where t.aria_cd = d.aria_cd and t.dia_id = d.dia_id)

LOOP

DELETE FROM d_traindata WHERE aria_cd = train.aria_cd AND dia_id = train.dia_id AND train_no = train.train_no AND train_div = train.train_div;
DELETE FROM d_train WHERE aria_cd = train.aria_cd AND dia_id = train.dia_id AND train_no = train.train_no AND train_div = train.train_div;

END LOOP;

return 0;

END;
$BODY$
LANGUAGE 'plpgsql' VOLATILE;

select fix_error_by_dia(263);
