import { NavLink } from 'react-router-dom';

type Props = {
  username: string;
  onLogout: () => void;
};

export default function NavBar({ username, onLogout }: Props) {
  return (
    <header className="nav">
      <div className="brand">Payments Stream Ops</div>
      <nav>
        <NavLink to="/" end>Overview</NavLink>
        <NavLink to="/errors">Errors</NavLink>
        <NavLink to="/dlq">DLQ Console</NavLink>
        <NavLink to="/config">Config</NavLink>
      </nav>
      <div className="userbox">
        <span>{username}</span>
        <button onClick={onLogout}>Logout</button>
      </div>
    </header>
  );
}
