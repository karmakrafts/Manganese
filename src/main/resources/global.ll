source_filename = "global"

declare void @abort()

define void @fe.panic(i8* %message) {
    %current = %message
    %cond0 = icmp ne i64 %message, 0
    br i1 %cond0, label %if_not_null, label %return
if_not_null:
    %cond1 = icmp ne i8 *%current, 0
    br i1 %cond1, label %if_valid, label %return
if_valid:
    ret void
return:
    ret void
}