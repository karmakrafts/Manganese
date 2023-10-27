source_filename = "global.ll"

define i32 @foobar(i32 %a, i32 %b) {
entry:
    %asq = mul i32 %a, %a
    %bsq = mul i32 %b, %b
    %res = add i32 %asq, %bsq
    ret i32 %res
}

declare void @abort()