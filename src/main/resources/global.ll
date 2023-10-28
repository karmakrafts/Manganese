source_filename = "global.ll"

define i32 @hello(i32 %a, i32 %b) {
entry:
    %0 = mul i32 %a, %a
    %1 = mul i32 %b, %b
    %2 = add i32 %0, %1
	ret i32 %2
}
