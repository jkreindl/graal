; ModuleID = 'complex.c'
source_filename = "complex.c"
target datalayout = "e-m:e-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-unknown-linux-gnu"

%struct.anon = type { i32, float, double }

@glob = internal global i32 0, align 4
@s = internal global %struct.anon zeroinitializer, align 8

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @valueFunc(i32) #0 {
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  store i32 %0, i32* %3, align 4
  %4 = load i32, i32* %3, align 4
  switch i32 %4, label %10 [
    i32 0, label %5
    i32 1, label %6
    i32 2, label %7
    i32 3, label %8
    i32 4, label %9
  ]

; <label>:5:                                      ; preds = %1
  store i32 5, i32* %2, align 4
  br label %11

; <label>:6:                                      ; preds = %1
  store i32 4, i32* %2, align 4
  br label %11

; <label>:7:                                      ; preds = %1
  store i32 3, i32* %2, align 4
  br label %11

; <label>:8:                                      ; preds = %1
  store i32 1, i32* %2, align 4
  br label %11

; <label>:9:                                      ; preds = %1
  store i32 0, i32* %2, align 4
  br label %11

; <label>:10:                                     ; preds = %1
  store i32 -1, i32* %2, align 4
  br label %11

; <label>:11:                                     ; preds = %10, %9, %8, %7, %6, %5
  %12 = load i32, i32* %2, align 4
  ret i32 %12
}

; Function Attrs: noinline nounwind optnone uwtable
define dso_local void @voidFunc(i32) #0 {
  %2 = alloca i32, align 4
  store i32 %0, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = icmp slt i32 %3, 10
  br i1 %4, label %5, label %8

; <label>:5:                                      ; preds = %1
  %6 = load i32, i32* %2, align 4
  %7 = or i32 %6, 32
  store i32 %7, i32* %2, align 4
  br label %8

; <label>:8:                                      ; preds = %5, %1
  %9 = load i32, i32* %2, align 4
  %10 = sdiv i32 %9, 2
  store i32 %10, i32* @glob, align 4
  %11 = load i32, i32* @glob, align 4
  store i32 %11, i32* getelementptr inbounds (%struct.anon, %struct.anon* @s, i32 0, i32 0), align 8
  %12 = load i32, i32* %2, align 4
  %13 = sitofp i32 %12 to double
  %14 = fmul double 4.200000e+00, %13
  %15 = fptrunc double %14 to float
  store float %15, float* getelementptr inbounds (%struct.anon, %struct.anon* @s, i32 0, i32 1), align 4
  %16 = load i32, i32* @glob, align 4
  %17 = sitofp i32 %16 to double
  %18 = call double @sqrt(double %17) #2
  store double %18, double* getelementptr inbounds (%struct.anon, %struct.anon* @s, i32 0, i32 2), align 8
  ret void
}

; Function Attrs: nounwind
declare dso_local double @sqrt(double) #1

; Function Attrs: noinline nounwind optnone uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  %3 = alloca i32, align 4
  %4 = alloca i32, align 4
  %5 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  store i32 0, i32* %2, align 4
  store i32 0, i32* %3, align 4
  store i32 0, i32* %4, align 4
  br label %6

; <label>:6:                                      ; preds = %18, %0
  %7 = load i32, i32* %3, align 4
  %8 = icmp slt i32 %7, 6
  br i1 %8, label %9, label %24

; <label>:9:                                      ; preds = %6
  %10 = load i32, i32* %3, align 4
  %11 = call i32 @valueFunc(i32 %10)
  store i32 %11, i32* %5, align 4
  %12 = load i32, i32* %2, align 4
  %13 = load i32, i32* @glob, align 4
  %14 = add nsw i32 %12, %13
  store i32 %14, i32* %2, align 4
  %15 = load i32, i32* %4, align 4
  %16 = load i32, i32* %5, align 4
  %17 = add nsw i32 %15, %16
  call void @voidFunc(i32 %17)
  br label %18

; <label>:18:                                     ; preds = %9
  %19 = load i32, i32* %3, align 4
  %20 = add nsw i32 %19, 1
  store i32 %20, i32* %3, align 4
  %21 = load i32, i32* %4, align 4
  %22 = load i32, i32* %4, align 4
  %23 = add nsw i32 %22, %21
  store i32 %23, i32* %4, align 4
  br label %6

; <label>:24:                                     ; preds = %6
  %25 = load i32, i32* %2, align 4
  ret i32 %25
}

attributes #0 = { noinline nounwind optnone uwtable "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nounwind "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "less-precise-fpmad"="false" "no-frame-pointer-elim"="true" "no-frame-pointer-elim-non-leaf" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+fxsr,+mmx,+sse,+sse2,+x87" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { nounwind }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"clang version 8.0.0 "}
