// Shape of one entry in the Inkwell-user carousel. It lives outside
// $lib/server so the component that renders it can import the type
// without pulling a server-only module into the client graph.

export type InkwellUser = {
  handle: string;
  displayName: string | null;
  avatar: string | null;
};
