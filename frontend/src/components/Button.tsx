type ButtonProps = {
  text: string;
  type?: "button" | "submit";
  onClick?: () => void;
};

function Button({
  text,
  type = "button",
  onClick,
}: ButtonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition"
    >
      {text}
    </button>
  );
}

export default Button;