type ButtonProps = {
  text: string;
  type?: "button" | "submit";
};

function Button({
  text,
  type = "button",
}: ButtonProps) {
  return (
    <button
      type={type}
      className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 rounded-lg transition"
    >
      {text}
    </button>
  );
}

export default Button;