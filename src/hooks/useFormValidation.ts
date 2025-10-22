import { useState, useCallback } from 'react';

type FormValues = Record<string, string>;
type FormErrors = Record<string, string>;
type FormTouched = Record<string, boolean>;
type ValidationRules = Record<
  string,
  (value: string, allValues?: FormValues) => string | null
>;

interface UseFormValidationReturn {
  values: FormValues;
  errors: FormErrors;
  touched: FormTouched;
  isValid: boolean;
  setFieldValue: (field: string, value: string) => void;
  setFieldTouched: (field: string) => void;
}

export const useFormValidation = (
  initialValues: FormValues,
  validationRules: ValidationRules,
): UseFormValidationReturn => {
  const [values, setValues] = useState<FormValues>(initialValues);
  const [errors, setErrors] = useState<FormErrors>({});
  const [touched, setTouched] = useState<FormTouched>({});

  const validateField = useCallback(
    (name: string, value: string, allValues: FormValues): string | null => {
      const validator = validationRules[name];
      if (!validator) {
        return null;
      }
      return validator(value, allValues);
    },
    [validationRules],
  );

  const setFieldValue = useCallback(
    (field: string, value: string): void => {
      setValues(prev => {
        const newValues = { ...prev, [field]: value };

        // 값 업데이트 후 즉시 검증
        setTouched(prevTouched => {
          if (prevTouched[field]) {
            const error = validateField(field, value, newValues);
            setErrors(prevErrors => ({
              ...prevErrors,
              [field]: error || '',
            }));
          }
          return prevTouched;
        });

        return newValues;
      });
    },
    [validateField],
  );

  const setFieldTouched = useCallback(
    (field: string): void => {
      setTouched(prev => ({ ...prev, [field]: true }));

      // 현재 values 사용하여 검증
      const error = validateField(field, values[field] || '', values);
      setErrors(prev => ({ ...prev, [field]: error || '' }));
    },
    [validateField, values],
  );

  const isValid = Object.keys(values).every(key => {
    if (!touched[key]) {
      return false;
    }
    return !errors[key];
  });

  return {
    values,
    errors,
    touched,
    isValid,
    setFieldValue,
    setFieldTouched,
  };
};
