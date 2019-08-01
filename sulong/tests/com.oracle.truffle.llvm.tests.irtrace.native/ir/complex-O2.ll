; ModuleID = 'complex.c'
source_filename = "complex.c"
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

@glob = internal unnamed_addr global i32 0, align 4
@switch.table.valueFunc = private unnamed_addr constant [5 x i32] [i32 5, i32 4, i32 3, i32 1, i32 0], align 4

; Function Attrs: norecurse nounwind readnone uwtable
define dso_local i32 @valueFunc(i32) local_unnamed_addr #0 {
  %2 = icmp ult i32 %0, 5
  br i1 %2, label %3, label %7

; <label>:3:                                      ; preds = %1
  %4 = sext i32 %0 to i64
  %5 = getelementptr inbounds [5 x i32], [5 x i32]* @switch.table.valueFunc, i64 0, i64 %4
  %6 = load i32, i32* %5, align 4
  ret i32 %6

; <label>:7:                                      ; preds = %1
  ret i32 -1
}

; Function Attrs: nounwind uwtable
define dso_local void @voidFunc(i32) local_unnamed_addr #1 {
  %2 = icmp slt i32 %0, 10
  %3 = or i32 %0, 32
  %4 = select i1 %2, i32 %3, i32 %0
  %5 = sdiv i32 %4, 2
  store i32 %5, i32* @glob, align 4, !tbaa !2
  %6 = icmp slt i32 %4, -1
  br i1 %6, label %7, label %10, !prof !6

; <label>:7:                                      ; preds = %1
  %8 = sitofp i32 %5 to double
  %9 = tail call double @sqrt(double %8) #3
  br label %10

; <label>:10:                                     ; preds = %1, %7
  ret void
}

; Function Attrs: nounwind
declare dso_local double @sqrt(double) local_unnamed_addr #2

; Function Attrs: nounwind uwtable
define dso_local i32 @main() local_unnamed_addr #1 {
  %1 = load i32, i32* @glob, align 4, !tbaa !2
  %2 = add nsw i32 %1, 85
  store i32 0, i32* @glob, align 4, !tbaa !2
  ret i32 %2
}

attributes #0 = { norecurse nounwind readnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nounwind uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { nounwind "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #3 = { nounwind }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"clang version 8.0.0 "}
!2 = !{!3, !3, i64 0}
!3 = !{!"int", !4, i64 0}
!4 = !{!"omnipotent char", !5, i64 0}
!5 = !{!"Simple C/C++ TBAA"}
!6 = !{!"branch_weights", i32 1, i32 2000}
