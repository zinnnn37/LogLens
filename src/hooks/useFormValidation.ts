import { useState } from 'react';

type FormValues = Record<string, string>;
type FormErrors = Record<string, string>;
type FormTouched = Record<string, boolean>;
type ValidationRules = Record<string, (value: string) => string | null>;

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

  const validateField = (name: string, value: string): string | null => {
    const validator = validationRules[name];
    if (!validator) {
      return null;
    }
    return validator(value);
  };

  const setFieldValue = (field: string, value: string): void => {
    setValues(prev => ({ ...prev, [field]: value }));

    if (touched[field]) {
      const error = validateField(field, value);
      setErrors(prev => ({ ...prev, [field]: error || '' }));
    }
  };

  const setFieldTouched = (field: string): void => {
    setTouched(prev => ({ ...prev, [field]: true }));

    const error = validateField(field, values[field] || '');
    setErrors(prev => ({ ...prev, [field]: error || '' }));
  };

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
