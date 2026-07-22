import styles from "./ErrorAlert.module.css";

interface ErrorAlertProps {
  message: string;
}

export default function ErrorAlert({ message }: ErrorAlertProps) {
  return (
    <div className={styles.container} role="alert">
      {message}
    </div>
  );
}