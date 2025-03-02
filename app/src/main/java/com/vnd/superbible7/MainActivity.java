package com.vnd.superbible7;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ExpandableListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.vnd.superbible7.chapter9.x5_Framebuffer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ProgramAdapter adapter;
    private List<Program> programList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            // Show app icon in default action bar in vertical mode
            getSupportActionBar().setIcon(R.mipmap.ic_launcher);
            getSupportActionBar().setDisplayShowHomeEnabled(true);  // To show the icon
            // getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Optional, if you want a back button
        }

        Button btnHome = findViewById(R.id.btnHome);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnHome.setOnClickListener(view -> {
        });

        btnAbout.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        programList = createAssignments();
        adapter = new ProgramAdapter(this, programList);
        expandableListView.setAdapter(adapter);

        // Handle click on child
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            Program program = (Program) adapter.getChild(groupPosition, childPosition);
            if (program.getClassName().equals(com.vnd.superbible7.chapter6.x2_Subroutines.class.getName())) {
                showNoSupportDialog();
                return true; // Return true to disallow group expansion/collapse
            } else if (program.getClassName().equals(com.vnd.superbible7.chapter5.x9_FragmentList.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter9.x6_LayeredRendering.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter9.x7_Stereo.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter9.x13_HDRToneMapping.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter9.x14_HDRBloom.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter10.x3_DepthOfField.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter10.x4_ComputeShaderFlocking.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter11.x3_RGTCCompressor.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x4_BumpMapping.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x9_ShadowMapping.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x11_DeferredShading.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x12_ScreenSpaceAmbientOcclusion.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x14_RayTracing.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x15_DistanceField.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter13.x17_BitmapFontRendering.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter14.x2_PacketBuffer.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter14.x3_IndirectMaterials.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter14.x4_IndirectCulling.class.getName()) ||
                    program.getClassName().equals(com.vnd.superbible7.chapter14.x5_ConstantStreaming.class.getName())) {
                showNotYetConvertedDialog();
                return true; // Return true to disallow group expansion/collapse
            }
            return startOpenGLActivity(program);
        });

        // Handle click on parent
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            Program program = programList.get(groupPosition);
            if (program.getChildren().isEmpty()) {
                showNoProgramsDialog();
                return true; // Return true to disallow group expansion/collapse
            }
            return false;  // Return false to allow group expansion/collapse
        });
    }

    private boolean startOpenGLActivity(Program program) {
        if (program.getChildren().isEmpty()) {
            Intent intent = new Intent(MainActivity.this, GLESActivity.class);
            intent.putExtra("className", program.getClassName());
            startActivity(intent);
            return true; // Return true to disallow group expansion/collapse
        }
        return false; // Return false to allow group expansion/collapse
    }

    private void showNoProgramsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage("There are no OpenGL programs in this chapter.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showNoSupportDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage("Subroutines" + " are not supported in OpenGL ES.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showNotYetConvertedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage("This program is not yet converted in OpenGL ES.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private List<Program> createAssignments() {
        List<Program> programs = new ArrayList<>();

        Program chapter1 = new Program("Chapter 1. Introduction", null);
        programs.add(chapter1);

        Program chapter2 = new Program("Chapter 2. Our First OpenGL Program", null);
        chapter2.addChild(new Program("1. Simple Clear", com.vnd.superbible7.chapter2.x1_SimpleClear.class.getName()));
        chapter2.addChild(new Program("2. Single Point", com.vnd.superbible7.chapter2.x2_SinglePoint.class.getName()));
        chapter2.addChild(new Program("3. Single Triangle", com.vnd.superbible7.chapter2.x3_SingleTriangle.class.getName()));
        programs.add(chapter2);

        Program chapter3 = new Program("Chapter 3. Following the Pipeline", null);
        chapter3.addChild(new Program("1. Moving Triangle", com.vnd.superbible7.chapter3.x1_MovingTriangle.class.getName()));
        chapter3.addChild(new Program("2. Tessellated Triangle", com.vnd.superbible7.chapter3.x2_TessellatedTriangle.class.getName()));
        chapter3.addChild(new Program("3. Tessellation and Geometry Shaders", com.vnd.superbible7.chapter3.x3_GeometryTessellatedTriangle.class.getName()));
        chapter3.addChild(new Program("4. Simple Triangle", com.vnd.superbible7.chapter3.x4_FragmentColor.class.getName()));
        programs.add(chapter3);

        Program chapter4 = new Program("Chapter 4. Math for 3D Graphics", null);
        programs.add(chapter4);

        Program chapter5 = new Program("Chapter 5. Data", null);
        chapter5.addChild(new Program("1. Spinning Cube", com.vnd.superbible7.chapter5.x1_SpinningCube.class.getName()));
        chapter5.addChild(new Program("2. Simple Texturing", com.vnd.superbible7.chapter5.x2_SimpleTexturing.class.getName()));
        chapter5.addChild(new Program("3. Texture Viewer", com.vnd.superbible7.chapter5.x3_TextureViewer.class.getName()));
        chapter5.addChild(new Program("4. Texture Coordinates", com.vnd.superbible7.chapter5.x4_TextureCoordinates.class.getName()));
        chapter5.addChild(new Program("5. Tunnel", com.vnd.superbible7.chapter5.x5_Tunnel.class.getName()));
        chapter5.addChild(new Program("6. Texture Wrap Modes", com.vnd.superbible7.chapter5.x6_TextureWrapModes.class.getName()));
        chapter5.addChild(new Program("7. Mirrored Repeat", com.vnd.superbible7.chapter5.x7_MirroredRepeat.class.getName()));
        chapter5.addChild(new Program("8. Alien Rain", com.vnd.superbible7.chapter5.x8_AlienRain.class.getName()));
        chapter5.addChild(new Program("9. TODO Fragment List", com.vnd.superbible7.chapter5.x9_FragmentList.class.getName()));
        programs.add(chapter5);

        Program chapter6 = new Program("Chapter 6. Shaders and Programs", null);
        chapter6.addChild(new Program("1. Program Information", com.vnd.superbible7.chapter6.x1_ProgramInfo.class.getName()));
        chapter6.addChild(new Program("2. Shader Subroutines", com.vnd.superbible7.chapter6.x2_Subroutines.class.getName()));
        programs.add(chapter6);

        Program chapter7 = new Program("Chapter 7. Vertex Processing and Drawing Commands", null);
        chapter7.addChild(new Program("1. Indexed Cube", com.vnd.superbible7.chapter7.x1_IndexedCube.class.getName()));
        chapter7.addChild(new Program("2. Grass", com.vnd.superbible7.chapter7.x2_Grass.class.getName()));
        chapter7.addChild(new Program("3. Instanced Attributes", com.vnd.superbible7.chapter7.x3_InstancedAttributes.class.getName()));
        chapter7.addChild(new Program("4. Asteroids", com.vnd.superbible7.chapter7.x4_Asteroids.class.getName()));
        chapter7.addChild(new Program("5. Spring-Mass Simulator", com.vnd.superbible7.chapter7.x5_SpringMass.class.getName()));
        chapter7.addChild(new Program("6. Clip Distance", com.vnd.superbible7.chapter7.x6_ClipDistance.class.getName()));
        programs.add(chapter7);

        Program chapter8 = new Program("Chapter 8. Primitive Processing", null);
        chapter8.addChild(new Program(" 1. Tessellation Modes", com.vnd.superbible7.chapter8.x1_TessellationModes.class.getName()));
        chapter8.addChild(new Program(" 2. Subdivision Modes", com.vnd.superbible7.chapter8.x2_SubdivisionModes.class.getName()));
        chapter8.addChild(new Program(" 3. Tessellated Cube", com.vnd.superbible7.chapter8.x3_TessellatedCube.class.getName()));
        chapter8.addChild(new Program(" 4. Displacement Mapping", com.vnd.superbible7.chapter8.x4_DisplacementMap.class.getName()));
        chapter8.addChild(new Program(" 5. Cubic Bezier Patch", com.vnd.superbible7.chapter8.x5_CubicBezierPatch.class.getName()));
        chapter8.addChild(new Program(" 6. Geometry Shader Culling", com.vnd.superbible7.chapter8.x6_GeometryShaderCulling.class.getName()));
        chapter8.addChild(new Program(" 7. Exploder", com.vnd.superbible7.chapter8.x7_Exploder.class.getName()));
        chapter8.addChild(new Program(" 8. Geometry Shader Tessellation", com.vnd.superbible7.chapter8.x8_GeometryShaderTessellation.class.getName()));
        chapter8.addChild(new Program(" 9. Normal Viewer", com.vnd.superbible7.chapter8.x9_NormalViewer.class.getName()));
        chapter8.addChild(new Program("10. Quad Rendering", com.vnd.superbible7.chapter8.x10_QuadRendering.class.getName()));
        chapter8.addChild(new Program("11. Multiple Viewports", com.vnd.superbible7.chapter8.x11_MultipleViewports.class.getName()));
        programs.add(chapter8);

        Program chapter9 = new Program("Chapter 9. Fragment Processing and the Framebuffer", null);
        chapter9.addChild(new Program(" 1. Perspective", com.vnd.superbible7.chapter9.x1_Perspective.class.getName()));
        chapter9.addChild(new Program(" 2. Multiple Scissors", com.vnd.superbible7.chapter9.x2_MultipleScissors.class.getName()));
        chapter9.addChild(new Program(" 3. Depth Clamping", com.vnd.superbible7.chapter9.x3_DepthClamping.class.getName()));
        chapter9.addChild(new Program(" 4. Blending Functions", com.vnd.superbible7.chapter9.x4_BlendingFunctions.class.getName()));
        chapter9.addChild(new Program(" 5. Basic Framebuffer Object", x5_Framebuffer.class.getName()));
        chapter9.addChild(new Program(" 6. TODO Layered Rendering", com.vnd.superbible7.chapter9.x6_LayeredRendering.class.getName()));
        chapter9.addChild(new Program(" 7. TODO Stereo", com.vnd.superbible7.chapter9.x7_Stereo.class.getName()));
        chapter9.addChild(new Program(" 8. Line Smoothing", com.vnd.superbible7.chapter9.x8_LineSmoothing.class.getName()));
        chapter9.addChild(new Program(" 9. Polygon Smoothing", com.vnd.superbible7.chapter9.x9_PolygonSmoothing.class.getName()));
        chapter9.addChild(new Program("10. Native MSAA", com.vnd.superbible7.chapter9.x10_NativeMSAA.class.getName()));
        chapter9.addChild(new Program("11. Sample Rate Shading", com.vnd.superbible7.chapter9.x11_SampleRateShading.class.getName()));
        chapter9.addChild(new Program("12. HDR Exposure", com.vnd.superbible7.chapter9.x12_HDRExposure.class.getName()));
        chapter9.addChild(new Program("13. TODO HDR Tone Mapping", com.vnd.superbible7.chapter9.x13_HDRToneMapping.class.getName()));
        chapter9.addChild(new Program("14. TODO HDR Bloom", com.vnd.superbible7.chapter9.x14_HDRBloom.class.getName()));
        chapter9.addChild(new Program("15. Star Field", com.vnd.superbible7.chapter9.x15_StarField.class.getName()));
        chapter9.addChild(new Program("16. Shaped Points", com.vnd.superbible7.chapter9.x16_ShapedPoints.class.getName()));
        programs.add(chapter9);

        Program chapter10 = new Program("Chapter 10. Compute Shaders", null);
        chapter10.addChild(new Program(" 1. 1D Prefix Sum", com.vnd.superbible7.chapter10.x1_1DPrefixSum.class.getName()));
        chapter10.addChild(new Program(" 2. 2D Prefix Sum", com.vnd.superbible7.chapter10.x2_2DPrefixSum.class.getName()));
        chapter10.addChild(new Program(" 3. TODO Depth Of Field", com.vnd.superbible7.chapter10.x3_DepthOfField.class.getName()));
        chapter10.addChild(new Program(" 4. TODO Compute Shader Flocking", com.vnd.superbible7.chapter10.x4_ComputeShaderFlocking.class.getName()));
        programs.add(chapter10);

        Program chapter11 = new Program("Chapter 11. Advanced Data Management", null);
        chapter11.addChild(new Program(" 1. Bindless Textures", com.vnd.superbible7.chapter11.x1_BindlessTextures.class.getName()));
        chapter11.addChild(new Program(" 2. Sparse Textures", com.vnd.superbible7.chapter11.x2_SparseTextures.class.getName()));
        chapter11.addChild(new Program(" 3. TODO RGTC Compressor", com.vnd.superbible7.chapter11.x3_RGTCCompressor.class.getName()));
        chapter11.addChild(new Program(" 4. High Quality Texture Filtering", com.vnd.superbible7.chapter11.x4_HighQualityTextureFiltering.class.getName()));
        programs.add(chapter11);

        Program chapter12 = new Program("Chapter 12. Controlling and Monitoring the Pipeline", null);
        programs.add(chapter12);

        Program chapter13 = new Program("Chapter 13. Rendering Techniques", null);
        chapter13.addChild(new Program(" 1. Phong Lighting", com.vnd.superbible7.chapter13.x1_PhongLighting.class.getName()));
        chapter13.addChild(new Program(" 2. Blinn-Phong Shading", com.vnd.superbible7.chapter13.x2_BlinnPhongShading.class.getName()));
        chapter13.addChild(new Program(" 3. Rim Lighting", com.vnd.superbible7.chapter13.x3_RimLighting.class.getName()));
        chapter13.addChild(new Program(" 4. TODO Bump Mapping", com.vnd.superbible7.chapter13.x4_BumpMapping.class.getName()));
        chapter13.addChild(new Program(" 5. Spherical Environment Map", com.vnd.superbible7.chapter13.x5_SphericalEnvironment.class.getName()));
        chapter13.addChild(new Program(" 6. Equirectangular Environment Map", com.vnd.superbible7.chapter13.x6_EquirectangularEnvironment.class.getName()));
        chapter13.addChild(new Program(" 7. Cubic Environment Map", com.vnd.superbible7.chapter13.x7_CubicEnvironmentMap.class.getName()));
        chapter13.addChild(new Program(" 8. Per-Pixel Gloss", com.vnd.superbible7.chapter13.x8_PerPixelGloss.class.getName()));
        chapter13.addChild(new Program(" 9. TODO Shadow Mapping", com.vnd.superbible7.chapter13.x9_ShadowMapping.class.getName()));
        chapter13.addChild(new Program("10. Toon Shading", com.vnd.superbible7.chapter13.x10_ToonShading.class.getName()));
        chapter13.addChild(new Program("11. TODO Deferred Shading", com.vnd.superbible7.chapter13.x11_DeferredShading.class.getName()));
        chapter13.addChild(new Program("12. TODO Screen-Space Ambient Occlusion", com.vnd.superbible7.chapter13.x12_ScreenSpaceAmbientOcclusion.class.getName()));
        chapter13.addChild(new Program("13. Julia Fractal", com.vnd.superbible7.chapter13.x13_JuliaFractal.class.getName()));
        chapter13.addChild(new Program("14. TODO Ray Tracing", com.vnd.superbible7.chapter13.x14_RayTracing.class.getName()));
        chapter13.addChild(new Program("15. TODO Distance Field", com.vnd.superbible7.chapter13.x15_DistanceField.class.getName()));
        chapter13.addChild(new Program("16. Distance Field Landscaping", com.vnd.superbible7.chapter13.x16_DistanceFieldLandscape.class.getName()));
        chapter13.addChild(new Program("17. TODO Bitmap Font Rendering", com.vnd.superbible7.chapter13.x17_BitmapFontRendering.class.getName()));
        programs.add(chapter13);

        Program chapter14 = new Program("Chapter 14. High-Performance OpenGL", null);
        chapter14.addChild(new Program(" 1. Parallel Particles", com.vnd.superbible7.chapter14.x1_ParallelParticles.class.getName()));
        chapter14.addChild(new Program(" 2. TODO Packet Buffer", com.vnd.superbible7.chapter14.x2_PacketBuffer.class.getName()));
        chapter14.addChild(new Program(" 3. TODO Indirect Materials", com.vnd.superbible7.chapter14.x3_IndirectMaterials.class.getName()));
        chapter14.addChild(new Program(" 4. TODO Indirect Culling", com.vnd.superbible7.chapter14.x4_IndirectCulling.class.getName()));
        chapter14.addChild(new Program(" 5. TODO Constant Streaming", com.vnd.superbible7.chapter14.x5_ConstantStreaming.class.getName()));
        chapter14.addChild(new Program(" 6. Persistent Mapped Fractal", com.vnd.superbible7.chapter14.x6_PersistentMappedFractal.class.getName()));
        programs.add(chapter14);

        Program chapter15 = new Program("Chapter 15. Debugging and Stability", null);
        programs.add(chapter15);

        return programs;
    }
}
