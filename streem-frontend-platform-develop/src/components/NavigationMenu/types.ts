export interface MenuItem {
  icon: React.ReactType;
  name?: string;
  path: string;
  menu?: {
    name: string;
    path: string;
  }[];
}
