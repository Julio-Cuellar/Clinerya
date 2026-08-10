export function Field(props: {
  name: string;
  label: string;
  type?: string;
  required?: boolean;
  autoComplete?: string;
  defaultValue?: string;
  readOnly?: boolean;
  placeholder?: string;
  options?: Array<{ value: string; label: string }>;
}) {
  const { label, options, ...inputProps } = props;

  return (
    <label className="field">
      <span>{label}</span>
      {options ? (
        <select
          name={inputProps.name}
          defaultValue={inputProps.defaultValue ?? ""}
          required={inputProps.required}
        >
          <option value="" disabled>
            Selecciona una opción
          </option>
          {options.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      ) : (
        <input {...inputProps} />
      )}
    </label>
  );
}
