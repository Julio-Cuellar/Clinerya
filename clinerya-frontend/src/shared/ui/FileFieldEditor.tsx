import { useRef, useState } from "react";
import { IconFileTypePdf, IconPhoto, IconTrash, IconUpload } from "@tabler/icons-react";
import { attachmentsApi, getFriendlyError } from "@shared/api/api";
import { parseAttachments, serializeAttachments, type AttachmentMeta } from "@modules/records/types";

const ACCEPTED_TYPES = ".pdf,.jpg,.jpeg,.png";

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function FileFieldEditor({
  patientId,
  clinicId,
  elementId,
  value,
  onChange,
  readOnly = false
}: {
  patientId: string;
  clinicId: string;
  elementId: string;
  value: string | undefined;
  onChange: (next: string) => void;
  readOnly?: boolean;
}) {
  const attachments = parseAttachments(value);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFiles = async (files: FileList | null) => {
    if (readOnly) return;
    if (!files || files.length === 0) return;
    setUploading(true);
    setError("");
    try {
      const uploaded: AttachmentMeta[] = [];
      for (const file of Array.from(files)) {
        uploaded.push(await attachmentsApi.upload(patientId, clinicId, elementId, file));
      }
      onChange(serializeAttachments([...attachments, ...uploaded]));
    } catch (caught) {
      setError(getFriendlyError(caught));
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  const handleRemove = async (attachment: AttachmentMeta) => {
    if (readOnly) return;
    setError("");
    try {
      await attachmentsApi.remove(patientId, attachment.id);
      onChange(serializeAttachments(attachments.filter((item) => item.id !== attachment.id)));
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  const handleView = async (attachment: AttachmentMeta) => {
    setError("");
    try {
      const blob = await attachmentsApi.downloadBlob(patientId, attachment.id);
      const url = URL.createObjectURL(blob);
      window.open(url, "_blank", "noopener,noreferrer");
    } catch (caught) {
      setError(getFriendlyError(caught));
    }
  };

  return (
    <div className="file-field">
      {attachments.length > 0 && (
        <ul className="file-field-list">
          {attachments.map((attachment) => (
            <li key={attachment.id} className="file-field-item">
              {attachment.contentType === "application/pdf" ? (
                <IconFileTypePdf size={16} aria-hidden="true" />
              ) : (
                <IconPhoto size={16} aria-hidden="true" />
              )}
              <button type="button" className="file-field-name" onClick={() => handleView(attachment)}>
                {attachment.originalFilename}
              </button>
              <span className="file-field-size">{formatSize(attachment.sizeBytes)}</span>
              {!readOnly && (
                <button type="button" className="icon-btn" aria-label="Eliminar archivo" onClick={() => handleRemove(attachment)}>
                  <IconTrash size={14} />
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {!readOnly && (
        <label className="btn ghost file-field-upload">
          <IconUpload size={14} aria-hidden="true" />
          {uploading ? "Subiendo..." : "Adjuntar archivo (PDF, JPG, PNG)"}
          <input
            ref={inputRef}
            type="file"
            accept={ACCEPTED_TYPES}
            multiple
            hidden
            disabled={uploading}
            onChange={(event) => handleFiles(event.target.files)}
          />
        </label>
      )}
      {error && <p className="alert error">{error}</p>}
    </div>
  );
}
