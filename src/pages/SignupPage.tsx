import { GalleryVerticalEnd } from 'lucide-react';
import SignupIllust from '@/assets/images/SignupIllust.png';
import { SignupForm } from '@/components/SignupForm';

const SignupPage = () => {
  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <div className="bg-secondary relative hidden lg:block">
        <div className="sticky top-0 h-screen">
          <img
            src={SignupIllust}
            alt="Image"
            className="h-full w-full object-contain p-40 dark:brightness-[0.2] dark:grayscale"
          />
        </div>
      </div>
      <div className="flex flex-col gap-4 p-6 md:p-10">
        <div className="flex justify-center gap-2 md:justify-start">
          <a href="#" className="flex items-center gap-2 font-medium">
            <div className="bg-secondary text-primary-foreground flex size-6 items-center justify-center rounded-md">
              <GalleryVerticalEnd className="size-4" />
            </div>
            LogLens
          </a>
        </div>
        <div className="flex flex-1 items-center justify-center">
          <div className="w-full max-w-xs pb-10">
            <SignupForm />
          </div>
        </div>
      </div>
    </div>
  );
};

export default SignupPage;
